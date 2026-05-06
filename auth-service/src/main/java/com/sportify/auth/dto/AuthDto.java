package com.sportify.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        public String username;

        @NotBlank
        @Email
        public String email;

        @NotBlank
        @Size(min = 8)
        public String password;

        public String fullName;
        public String phone;
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        public String username;
        @NotBlank
        public String password;
    }

    @Data
    public static class TokenResponse {
        public String accessToken;
        public String refreshToken;
        public long expiresIn;
        public String tokenType = "Bearer";
    }

    @Data
    public static class UserProfileResponse {
        public Long id;
        public String username;
        public String email;
        public String fullName;
        public String phone;
        public String status;
    }
}
