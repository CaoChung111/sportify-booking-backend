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
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.Map;

@ApplicationScoped
public class AuthService {

    @ConfigProperty(name = "quarkus.keycloak.admin-client.server-url")
    String keycloakServerUrl;

    @ConfigProperty(name = "quarkus.keycloak.admin-client.realm")
    String realm;

    @ConfigProperty(name = "keycloak.public-client-id", defaultValue = "sportify-app")
    String publicClientId;

    @Inject
    Keycloak keycloakAdmin;

    /**
     * Đăng ký tài khoản mới:
     * 1. Tạo user trên Keycloak
     * 2. Lưu bản ghi User vào database local
     */
    @Transactional
    public AuthDto.UserProfileResponse register(AuthDto.RegisterRequest request) {
        // Check duplicate in local DB
        if (User.findByEmail(request.email) != null) {
            throw ServiceException.conflict("Email already registered: " + request.email);
        }

        // Build user representation
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password);

        UserRepresentation kcUser = new UserRepresentation();
        kcUser.setUsername(request.username);
        kcUser.setEmail(request.email);
        kcUser.setFirstName(request.fullName != null ? request.fullName : "");
        kcUser.setLastName("");
        kcUser.setEnabled(true);
        kcUser.setEmailVerified(true);
        kcUser.setCredentials(Collections.singletonList(credential));

        // Create user in Keycloak bằng đối tượng đã được Inject
        Response kcResponse = keycloakAdmin.realm(realm).users().create(kcUser);

        if (kcResponse.getStatus() == 409) {
            throw ServiceException.conflict("Username already exists on Keycloak: " + request.username);
        }

        if (kcResponse.getStatus() != 201) {
            // ĐỌC LỖI CHI TIẾT TỪ KEYCLOAK ĐỂ BIẾT TẠI SAO BỊ 400
            String errorDetail = kcResponse.readEntity(String.class);
            throw ServiceException.badRequest("Keycloak user creation failed: 400 - Chi tiết: " + errorDetail);
        }

        // Extract Keycloak user ID from Location header
        String location = kcResponse.getHeaderString("Location");
        String keycloakId = location.substring(location.lastIndexOf('/') + 1);

        // Persist local profile
        User user = new User();
        user.username = request.username;
        user.email = request.email;
        user.phone = request.phone != null ? request.phone : "";
        user.fullName = request.fullName;
        user.keycloakId = keycloakId;
        user.status = User.Status.ACTIVE;
        user.persist();

        return toProfileResponse(user);
    }

    /**
     * Đăng nhập: gọi Keycloak Token endpoint (Password Grant)
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
                // ĐỌC LỖI CHI TIẾT TỪ KEYCLOAK
                String errorDetail = response.readEntity(String.class);
                throw ServiceException.badRequest("Login failed: " + response.getStatus() + " - Chi tiết: " + errorDetail);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.readEntity(Map.class);
            return mapTokenResponse(body);
        } finally {
            httpClient.close();
        }
    }

    /**
     * Refresh access token
     */
    public AuthDto.TokenResponse refreshToken(String refreshToken) {
        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        Form form = new Form()
                .param("grant_type", "refresh_token")
                .param("client_id", publicClientId)
                .param("refresh_token", refreshToken);

        Client httpClient = ClientBuilder.newClient();
        try {
            Response response = httpClient.target(tokenUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.form(form));

            if (response.getStatus() != 200) {
                throw ServiceException.badRequest("Invalid or expired refresh token");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.readEntity(Map.class);
            return mapTokenResponse(body);
        } finally {
            httpClient.close();
        }
    }

    /**
     * Lấy profile user từ keycloakId (sub claim)
     */
    public AuthDto.UserProfileResponse getProfile(String keycloakId) {
        User user = User.findByKeycloakId(keycloakId);
        if (user == null) {
            throw ServiceException.notFound("User", 0L);
        }
        return toProfileResponse(user);
    }

    /**
     * Cập nhật profile user
     */
    @Transactional
    public AuthDto.UserProfileResponse updateProfile(String keycloakId, AuthDto.RegisterRequest request) {
        User user = User.findByKeycloakId(keycloakId);
        if (user == null) {
            throw ServiceException.notFound("User", 0L);
        }
        if (request.fullName != null) user.fullName = request.fullName;
        if (request.phone != null) user.phone = request.phone;
        user.persist();
        return toProfileResponse(user);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private AuthDto.TokenResponse mapTokenResponse(Map<String, Object> body) {
        AuthDto.TokenResponse token = new AuthDto.TokenResponse();
        token.accessToken = (String) body.get("access_token");
        token.refreshToken = (String) body.get("refresh_token");
        Object expiresIn = body.get("expires_in");
        token.expiresIn = expiresIn instanceof Number ? ((Number) expiresIn).longValue() : 300L;
        token.tokenType = "Bearer";
        return token;
    }

    private AuthDto.UserProfileResponse toProfileResponse(User user) {
        AuthDto.UserProfileResponse res = new AuthDto.UserProfileResponse();
        res.id = user.id;
        res.username = user.username;
        res.email = user.email;
        res.fullName = user.fullName;
        res.phone = user.phone;
        res.status = user.status.name();
        return res;
    }
}