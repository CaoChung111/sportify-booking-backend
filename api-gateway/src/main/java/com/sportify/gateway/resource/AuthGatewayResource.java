package com.sportify.gateway.resource;

import com.sportify.gateway.client.AuthServiceClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth Gateway", description = "Proxy → auth-service (port 8081)")
public class AuthGatewayResource {

    @Inject
    @RestClient
    AuthServiceClient authClient;

    @POST
    @Path("/register")
    @PermitAll
    @Operation(summary = "Đăng ký tài khoản mới")
    public Response register(Object body) {
        return authClient.register(body);
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Đăng nhập — nhận JWT Access Token")
    public Response login(Object body) {
        return authClient.login(body);
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Làm mới Access Token bằng Refresh Token")
    public Response refresh(Object body) {
        return authClient.refresh(body);
    }

    @GET
    @Path("/me")
    @Operation(summary = "Lấy thông tin profile của user đang đăng nhập")
    public Response getProfile(@Context HttpHeaders headers) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return authClient.getProfile(auth);
    }

    @PUT
    @Path("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân (fullName, phone)")
    public Response updateProfile(@Context HttpHeaders headers, Object body) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return authClient.updateProfile(auth, body);
    }
}
