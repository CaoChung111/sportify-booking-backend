package com.sportify.auth.service;

import com.sportify.auth.dto.AuthDto;
import com.sportify.auth.entity.User;
import com.sportify.common.exception.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.panache.common.Page;

@ApplicationScoped
public class AdminUserService {

    private static final Logger LOG = Logger.getLogger(AdminUserService.class);

    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm")
    String realm;

    @Inject
    Keycloak keycloakAdmin;

    // ── Luồng Xem (R) — GET /api/v1/auth/admin/users ────────────────────────

    /**
     * Tìm kiếm danh sách người dùng theo keyword (username hoặc email).
     * Nếu keyword rỗng/null → trả toàn bộ danh sách (có phân trang).
     * Nếu không tìm thấy → trả danh sách rỗng kèm message (KHÔNG lỗi 404).
     */
    public Map<String, Object> searchUsers(String keyword, int page, int size) {
        List<User> users;
        long totalElements;

        if (keyword == null || keyword.isBlank()) {
            totalElements = User.count();
            users = User.findAll()
                    .page(Page.of(page, size))
                    .list();
        } else {
            String pattern = "%" + keyword.toLowerCase() + "%";
            totalElements = User.count(
                    "LOWER(username) LIKE ?1 OR LOWER(email) LIKE ?1", pattern);
            users = User.find(
                            "LOWER(username) LIKE ?1 OR LOWER(email) LIKE ?1", pattern)
                    .page(Page.of(page, size))
                    .list();
        }

        List<AuthDto.UserDto> dtoList = users.stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", dtoList);
        result.put("currentPage", page);
        result.put("pageSize", size);
        result.put("totalElements", totalElements);
        result.put("totalPages", (int) Math.ceil((double) totalElements / size));

        if (dtoList.isEmpty() && keyword != null && !keyword.isBlank()) {
            result.put("message", "Không tìm thấy người dùng phù hợp với từ khóa: " + keyword);
        }

        return result;
    }

    // ── Luồng Sửa (U) — PATCH /api/v1/auth/admin/users/{id}/status ──────────

    /**
     * Cập nhật trạng thái tài khoản người dùng:
     * 1. Kiểm tra user tồn tại trong DB
     * 2. Kiểm tra user bị thao tác có phải ADMIN không (qua Keycloak roles)
     *    → Nếu là ADMIN thì từ chối (luồng thay thế 4a)
     * 3. Gọi Keycloak Admin Client set enabled=true/false theo keycloakId
     *    → Lỗi thì throw ServiceException (luồng thay thế 5a)
     * 4. Cập nhật status trong DB local
     */
    @Transactional
    public AuthDto.UserDto updateUserStatus(Long userId, String newStatus) {
        // 1. Validate status value
        User.Status targetStatus;
        try {
            targetStatus = User.Status.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ServiceException.badRequest(
                    "Trạng thái không hợp lệ: " + newStatus + ". Chỉ chấp nhận ACTIVE hoặc INACTIVE");
        }

        // 2. Tìm user trong DB
        User user = User.findById(userId);
        if (user == null) {
            throw ServiceException.notFound("User", userId);
        }

        // 3. Kiểm tra user bị thao tác có phải ADMIN không (qua Keycloak)
        //    → Luồng thay thế 4a: từ chối khóa tài khoản ADMIN khác
        if (targetStatus == User.Status.INACTIVE) {
            checkNotAdmin(user);
        }

        // 4. Gọi Keycloak Admin Client cập nhật enabled status
        boolean enabled = (targetStatus == User.Status.ACTIVE);
        updateKeycloakUserStatus(user.keycloakId, enabled);

        // 5. Cập nhật status trong DB local
        user.status = targetStatus;
        user.persist();

        LOG.infof("User [%s] (id=%d) status updated to %s", user.username, user.id, targetStatus);

        return toUserDto(user);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Kiểm tra user có role ADMIN trên Keycloak không.
     * Nếu có → throw ServiceException (luồng thay thế 4a).
     */
    private void checkNotAdmin(User user) {
        try {
            List<RoleRepresentation> roles = keycloakAdmin.realm(realm)
                    .users().get(user.keycloakId)
                    .roles().realmLevel().listEffective();

            boolean isAdmin = roles.stream()
                    .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()));

            if (isAdmin) {
                throw ServiceException.badRequest(
                        "Không thể khóa tài khoản của quản trị viên khác (username: " + user.username + ")");
            }
        } catch (ServiceException se) {
            throw se; // re-throw ServiceException as-is
        } catch (Exception e) {
            LOG.warnf(e, "Không thể kiểm tra role Keycloak cho user [%s]", user.keycloakId);
            throw ServiceException.badRequest(
                    "Không thể xác minh quyền của người dùng. Vui lòng thử lại sau.");
        }
    }

    /**
     * Gọi Keycloak Admin Client set enabled=true/false.
     * Đây là bước BẮT BUỘC vì luồng login hiện tại đi qua Keycloak trực tiếp,
     * không kiểm tra cột status trong DB → chỉ đổi DB sẽ không chặn được đăng nhập.
     */
    private void updateKeycloakUserStatus(String keycloakId, boolean enabled) {
        try {
            UserRepresentation kcUser = keycloakAdmin.realm(realm)
                    .users().get(keycloakId).toRepresentation();
            kcUser.setEnabled(enabled);
            keycloakAdmin.realm(realm).users().get(keycloakId).update(kcUser);
            LOG.infof("Keycloak user [%s] enabled=%s", keycloakId, enabled);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to update Keycloak user status for [%s]", keycloakId);
            throw new ServiceException(500,
                    "Cập nhật trạng thái trên Keycloak thất bại. Vui lòng thử lại sau.");
        }
    }

    private AuthDto.UserDto toUserDto(User user) {
        AuthDto.UserDto dto = new AuthDto.UserDto();
        dto.id = user.id;
        dto.username = user.username;
        dto.email = user.email;
        dto.fullName = user.fullName;
        dto.phone = user.phone;
        dto.status = user.status.name();
        dto.createdAt = user.createdAt != null ? user.createdAt.toString() : null;
        return dto;
    }
}