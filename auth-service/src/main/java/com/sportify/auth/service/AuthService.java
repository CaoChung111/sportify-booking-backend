package com.sportify.auth.service;

import com.sportify.auth.dto.AuthDto;
import com.sportify.auth.entity.User;
import com.sportify.common.exception.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @ConfigProperty(name = "quarkus.keycloak.admin-client.server-url")
    String keycloakServerUrl;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm")
    String realm;

    @ConfigProperty(name = "keycloak.public-client-id", defaultValue = "sportify-app")
    String publicClientId;

    @Inject
    Keycloak keycloakAdmin;

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Đăng ký tài khoản mới:
     * 1. Kiểm tra trùng email trong DB nội bộ
     * 2. Tạo user trên Keycloak (Admin API)
     * 3. Lấy Keycloak ID từ Location header
     * 4. Lưu bản ghi User vào database local
     */
    @Transactional
    public AuthDto.UserProfileResponse register(AuthDto.RegisterRequest request) {
        if (request.phone != null && User.find("phone", request.phone).firstResult() != null) {
            throw ServiceException.conflict("Phone number already registered");
        }
        // 1. Kiểm tra trùng email trong local DB
        if (User.findByEmail(request.email) != null) {
            throw ServiceException.conflict("Email already registered: " + request.email);
        }

        // 2. Build credential và user representation cho Keycloak
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password);

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(request.username);
        kcUser.setEmail(request.email);
        kcUser.setFirstName(request.fullName != null && !request.fullName.isBlank() ? request.fullName : request.username);
        kcUser.setLastName(" ");
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);
        kcUser.setCredentials(Collections.singletonList(credential));

        // 3. Gọi Keycloak Admin API tạo user
        Response kcResponse = keycloakAdmin.realm(realm).users().create(kcUser);

        if (kcResponse.getStatus() == 409) {
            throw ServiceException.conflict("Username already exists on Keycloak: " + request.username);
        }
        if (kcResponse.getStatus() != 201) {
            String errorDetail = kcResponse.readEntity(String.class);
            throw ServiceException.badRequest("Keycloak user creation failed: " + errorDetail);
        }

        // 4. Lấy Keycloak ID từ Location header: .../users/{uuid}
        String location = kcResponse.getHeaderString("Location");
        String keycloakId = location.substring(location.lastIndexOf('/') + 1);

        // 5. Lưu hồ sơ vào local DB
        User user = new User();
        user.username = request.username;
        user.email    = request.email;
        user.phone = request.phone;
        user.fullName = request.fullName;
        user.keycloakId = keycloakId;
        user.status   = User.Status.ACTIVE;
        user.persist();

        return toProfileResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Đăng nhập:
     * Gọi Keycloak Token endpoint bằng OIDC Password Grant
     * Trả về Access Token + Refresh Token
     */
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        Form form = new Form()
                .param("grant_type", "password")
                .param("client_id", publicClientId)
                .param("username", request.username)
                .param("password", request.password);

        Client httpClient = ClientBuilder.newClient();
        try {
            Response response = httpClient.target(tokenUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.form(form));

            if (response.getStatus() != 200) {
                String errorDetail = response.readEntity(String.class);
                throw ServiceException.badRequest("Login failed: " + response.getStatus() + " — " + errorDetail);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.readEntity(Map.class);
            return mapTokenResponse(body);
        } finally {
            httpClient.close();
        }
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    /**
     * Làm mới Access Token:
     * Gọi Keycloak Token endpoint với grant_type=refresh_token
     * Nhận refreshToken từ request body (bảo mật hơn QueryParam)
     */
    public AuthDto.TokenResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        Form form = new Form()
                .param("grant_type", "refresh_token")
                .param("client_id", publicClientId)
                .param("refresh_token", request.refreshToken);

        Client httpClient = ClientBuilder.newClient();
        try {
            Response response = httpClient.target(tokenUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.form(form));

            if (response.getStatus() != 200) {
                String errorDetail = response.readEntity(String.class);
                throw ServiceException.badRequest("Invalid or expired refresh token: " + errorDetail);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.readEntity(Map.class);
            return mapTokenResponse(body);
        } finally {
            httpClient.close();
        }
    }

    // ── Get Profile ───────────────────────────────────────────────────────────

    /**
     * Lấy thông tin profile của user đang đăng nhập.
     * keycloakId được lấy từ JWT sub claim (inject qua JsonWebToken).
     */
    public AuthDto.UserProfileResponse getProfile(String keycloakId, Set<String> roles) {
        User user = User.findByKeycloakId(keycloakId);
        if (user == null) {
            throw ServiceException.notFound("User with keycloak_id", 0L);
        }
        return toProfileResponse(user, roles);
    }

    // ── Update Profile ────────────────────────────────────────────────────────

    /**
     * Cập nhật thông tin profile:
     * 1. Chỉ cho phép sửa fullName và phone.
     * 2. username và email KHÔNG được phép thay đổi qua API này.
     * 3. Nếu fullName thay đổi → đồng bộ lên Keycloak (firstName).
     */
    @Transactional
    public AuthDto.UserProfileResponse updateProfile(String keycloakId, AuthDto.UpdateProfileRequest request) {
        User user = User.findByKeycloakId(keycloakId);
        if (user == null) {
            throw ServiceException.notFound("User with keycloak_id", 0L);
        }

        boolean fullNameChanged = false;

        // Chỉ update những trường được phép
        if (request.fullName != null && !request.fullName.isBlank()) {
            if (!request.fullName.equals(user.fullName)) {
                user.fullName = request.fullName;
                fullNameChanged = true;
            }
        }
        if (request.phone != null && !request.phone.isBlank()) {
            // Kiểm tra phone không bị trùng với user khác
            User existingByPhone = User.find("phone = ?1 and id != ?2", request.phone, user.id).firstResult();
            if (existingByPhone != null) {
                throw ServiceException.conflict("Phone number already used by another account");
            }
            user.phone = request.phone;
        }

        user.persist();

        // Đồng bộ fullName lên Keycloak nếu thay đổi
        if (fullNameChanged) {
            syncFullNameToKeycloak(keycloakId, request.fullName);
        }

        LOG.infof("Profile updated for user [%s]: fullName=%s, phone=%s",
                user.username, user.fullName, user.phone);

        return toProfileResponse(user);
    }

    /**
     * Đồng bộ fullName lên Keycloak (cập nhật firstName).
     * Nếu thất bại chỉ log warning, không rollback local DB.
     */
    private void syncFullNameToKeycloak(String keycloakId, String fullName) {
        try {
            UserRepresentation kcUser = keycloakAdmin.realm(realm)
                    .users().get(keycloakId).toRepresentation();
            kcUser.setFirstName(fullName);
            keycloakAdmin.realm(realm).users().get(keycloakId).update(kcUser);
            LOG.infof("Synced fullName to Keycloak for user [%s]", keycloakId);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to sync fullName to Keycloak for user [%s]", keycloakId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthDto.TokenResponse mapTokenResponse(Map<String, Object> body) {
        AuthDto.TokenResponse token = new AuthDto.TokenResponse();
        token.accessToken  = (String) body.get("access_token");
        token.refreshToken = (String) body.get("refresh_token");
        Object expiresIn   = body.get("expires_in");
        token.expiresIn    = expiresIn instanceof Number ? ((Number) expiresIn).longValue() : 300L;
        token.tokenType    = "Bearer";
        return token;
    }

    private AuthDto.UserProfileResponse toProfileResponse(User user) {
        return toProfileResponse(user, Collections.emptySet());
    }

    private AuthDto.UserProfileResponse toProfileResponse(User user, Set<String> roles) {
        AuthDto.UserProfileResponse res = new AuthDto.UserProfileResponse();
        res.id        = user.id;
        res.username  = user.username;
        res.email     = user.email;
        res.fullName  = user.fullName;
        res.phone     = user.phone;
        res.roles     = roles;
        res.role      = resolvePrimaryRole(roles);
        res.status    = user.status.name();
        res.createdAt = user.createdAt != null ? user.createdAt.toString() : null;
        return res;
    }

    private String resolvePrimaryRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("USER")) {
            return "USER";
        }
        return roles.iterator().next();
    }
}
