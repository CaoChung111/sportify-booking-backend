package com.sportify.auth.resource;

import com.sportify.auth.dto.AuthDto;
import com.sportify.auth.service.AuthService;
import com.sportify.common.dto.ApiResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập và quản lý tài khoản người dùng")
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    // ── Đăng ký ──────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/register
     * Đăng ký tài khoản mới. Không yêu cầu xác thực.
     */
    @POST
    @Path("/register")
    @PermitAll
    @Operation(summary = "Đăng ký tài khoản mới")
    @APIResponse(responseCode = "201", description = "Đăng ký thành công")
    @APIResponse(responseCode = "409", description = "Email hoặc username đã tồn tại")
    public Response register(
            @Valid
            @RequestBody(description = "Thông tin đăng ký", required = true)
            AuthDto.RegisterRequest request) {

        AuthDto.UserProfileResponse profile = authService.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("User registered successfully", profile))
                .build();
    }

    // ── Đăng nhập ────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/login
     * Đăng nhập và nhận Access Token + Refresh Token.
     */
    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Đăng nhập — nhận JWT Access Token")
    @APIResponse(responseCode = "200", description = "Đăng nhập thành công")
    @APIResponse(responseCode = "400", description = "Sai username hoặc mật khẩu")
    public Response login(
            @Valid
            @RequestBody(description = "Thông tin đăng nhập", required = true)
            AuthDto.LoginRequest request) {

        AuthDto.TokenResponse tokens = authService.login(request);
        return Response.ok(ApiResponse.success("Login successful", tokens)).build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/refresh
     * Làm mới Access Token bằng Refresh Token.
     * Nhận qua request body (bảo mật hơn URL query param).
     */
    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Làm mới Access Token bằng Refresh Token")
    @APIResponse(responseCode = "200", description = "Token mới đã được cấp")
    @APIResponse(responseCode = "400", description = "Refresh token không hợp lệ hoặc đã hết hạn")
    public Response refresh(
            @Valid
            @RequestBody(description = "Refresh token", required = true)
            AuthDto.RefreshTokenRequest request) {

        AuthDto.TokenResponse tokens = authService.refreshToken(request);
        return Response.ok(ApiResponse.success("Token refreshed", tokens)).build();
    }

    // ── Lấy Profile ──────────────────────────────────────────────────────────

    /**
     * GET /api/v1/auth/me
     * Lấy thông tin profile của user đang đăng nhập.
     * Yêu cầu: Authorization: Bearer <access_token>
     */
    @GET
    @Path("/me")
    @Operation(summary = "Lấy thông tin profile của user đang đăng nhập")
    @APIResponse(responseCode = "200", description = "Thông tin profile")
    @APIResponse(responseCode = "401", description = "Chưa xác thực")
    @APIResponse(responseCode = "404", description = "Không tìm thấy profile")
    public Response getProfile() {
        System.out.println(">>> KEYCLOAK ID NHẬN ĐƯỢC: " + jwt.getSubject());
        String keycloakId = jwt.getSubject();
        AuthDto.UserProfileResponse profile = authService.getProfile(keycloakId);
        return Response.ok(ApiResponse.success(profile)).build();
    }

    // ── Cập nhật Profile ──────────────────────────────────────────────────────

    /**
     * PUT /api/v1/auth/me
     * Cập nhật thông tin cá nhân: chỉ fullName và phone.
     * username và email KHÔNG thể thay đổi qua endpoint này.
     * Yêu cầu: Authorization: Bearer <access_token>
     */
    @PUT
    @Path("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân (fullName, phone)")
    @APIResponse(responseCode = "200", description = "Cập nhật thành công")
    @APIResponse(responseCode = "401", description = "Chưa xác thực")
    @APIResponse(responseCode = "409", description = "Số điện thoại đã được dùng bởi tài khoản khác")
    public Response updateProfile(
            @Valid
            @RequestBody(description = "Thông tin cần cập nhật", required = true)
            AuthDto.UpdateProfileRequest request) {

        String keycloakId = jwt.getSubject();
        AuthDto.UserProfileResponse profile = authService.updateProfile(keycloakId, request);
        return Response.ok(ApiResponse.success("Profile updated successfully", profile)).build();
    }
}
