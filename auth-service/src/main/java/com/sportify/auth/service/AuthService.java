package com.sportify.auth.service;

import com.sportify.auth.dto.AuthDto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthService {

    /**
     * Xử lý đăng ký tài khoản mới
     */
    public AuthDto.UserProfileResponse register(AuthDto.RegisterRequest request) {
        // TODO: 1. Validate thông tin (nếu cần)
        // TODO: 2. Gọi Keycloak Admin Client để tạo user trên Keycloak
        // TODO: 3. Lưu thông tin user vào Database của service (nếu có bảng User riêng)

        return null; // Thay bằng đối tượng UserProfileResponse thực tế sau khi xử lý
    }

    /**
     * Xử lý đăng nhập
     */
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        // TODO: Gọi Keycloak Token API (Grant Type: Password) để lấy Access Token & Refresh Token

        return null; // Thay bằng đối tượng TokenResponse thực tế
    }

    /**
     * Cấp lại token mới dựa trên Refresh Token
     */
    public AuthDto.TokenResponse refreshToken(String refreshToken) {
        // TODO: Gọi Keycloak Token API (Grant Type: Refresh Token)

        return null; // Thay bằng đối tượng TokenResponse thực tế
    }

    /**
     * Lấy thông tin Profile của User đang đăng nhập
     */
    public AuthDto.UserProfileResponse getProfile(String keycloakId) {
        // TODO: Query Database hoặc gọi Keycloak để lấy thông tin chi tiết dựa vào keycloakId

        return null; // Thay bằng đối tượng UserProfileResponse thực tế
    }

    /**
     * Cập nhật thông tin Profile
     */
    public AuthDto.UserProfileResponse updateProfile(String keycloakId, AuthDto.RegisterRequest request) {
        // TODO: 1. Cập nhật thông tin trên Keycloak (nếu đổi email/password...)
        // TODO: 2. Cập nhật thông tin dưới Database local

        return null; // Thay bằng đối tượng UserProfileResponse sau khi đã update
    }
}