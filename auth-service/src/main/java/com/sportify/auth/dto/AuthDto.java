package com.sportify.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    // ── Register ─────────────────────────────────────────────────────────────

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        public String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        public String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        public String password;

        public String fullName;

        @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Invalid Vietnamese phone number")
        public String phone;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        public String username;

        @NotBlank(message = "Password is required")
        public String password;
    }

    // ── Refresh Token (via body — bảo mật hơn QueryParam) ────────────────────

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        public String refreshToken;
    }

    // ── Update Profile (chỉ cho phép sửa fullName & phone) ───────────────────

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        public String fullName;

        @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Invalid Vietnamese phone number")
        public String phone;
    }

    // ── Token Response ────────────────────────────────────────────────────────

    @Data
    public static class TokenResponse {
        public String accessToken;
        public String refreshToken;
        public long expiresIn;
        public String tokenType = "Bearer";
    }

    // ── User Profile Response ─────────────────────────────────────────────────

    @Data
    public static class UserProfileResponse {
        public Long id;
        public String username;
        public String email;
        public String fullName;
        public String phone;
        public String status;
        public String createdAt;
    }
}
