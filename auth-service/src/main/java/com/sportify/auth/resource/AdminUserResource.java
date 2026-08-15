package com.sportify.auth.resource;

import com.sportify.auth.dto.AuthDto;
import com.sportify.auth.service.AdminUserService;
import com.sportify.common.dto.ApiResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/api/v1/auth/admin/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Management", description = "Quản lý người dùng — chỉ dành cho Quản trị viên (UC16)")
public class AdminUserResource {

    @Inject
    AdminUserService adminUserService;

    // ── Luồng Xem (R) ───────────────────────────────────────────────────────

    /**
     * GET /api/v1/auth/admin/users
     * Xem danh sách người dùng, hỗ trợ tìm kiếm theo username/email + phân trang.
     * Không tìm thấy → trả danh sách rỗng kèm message, KHÔNG trả lỗi 404.
     */
    @GET
    @PermitAll
    @Operation(summary = "Xem danh sách người dùng (có tìm kiếm & phân trang)")
    @APIResponse(responseCode = "200", description = "Trả về danh sách người dùng")
    @APIResponse(responseCode = "401", description = "Chưa xác thực")
    @APIResponse(responseCode = "403", description = "Không có quyền truy cập")
    public Response getUsers(
            @QueryParam("keyword") String keyword,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {

        Map<String, Object> result = adminUserService.searchUsers(keyword, page, size);
        return Response.ok(ApiResponse.success("Danh sách người dùng", result)).build();
    }

    // ── Luồng Sửa (U) ───────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/auth/admin/users/{id}/status
     * Khóa hoặc kích hoạt tài khoản người dùng.
     * Body: { "status": "ACTIVE" | "INACTIVE" }
     * - Không thể khóa tài khoản ADMIN khác (luồng thay thế 4a)
     * - Lỗi Keycloak → trả lỗi rõ ràng (luồng thay thế 5a)
     */
    @PATCH
    @Path("/{id}/status")
    @PermitAll
    @Operation(summary = "Khóa hoặc kích hoạt tài khoản người dùng")
    @APIResponse(responseCode = "200", description = "Cập nhật trạng thái thành công")
    @APIResponse(responseCode = "400", description = "Trạng thái không hợp lệ hoặc cố khóa ADMIN")
    @APIResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    public Response updateUserStatus(
            @PathParam("id") Long id,
            @Valid AuthDto.UserStatusUpdateRequest request) {

        AuthDto.UserDto updatedUser = adminUserService.updateUserStatus(id, request.status);
        return Response.ok(ApiResponse.success("Cập nhật trạng thái tài khoản thành công", updatedUser)).build();
    }
}