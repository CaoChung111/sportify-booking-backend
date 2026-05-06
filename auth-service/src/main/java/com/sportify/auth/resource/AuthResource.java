package com.sportify.auth.resource;

import com.sportify.auth.dto.AuthDto;
import com.sportify.auth.service.AuthService;
import com.sportify.common.dto.ApiResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication")
public class AuthResource {

    @Inject AuthService authService;
    @Inject JsonWebToken jwt;

    @POST
    @Path("/register")
    @PermitAll
    @Operation(summary = "Register a new user")
    public Response register(@Valid AuthDto.RegisterRequest request) {
        AuthDto.UserProfileResponse profile = authService.register(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("User registered successfully", profile))
                .build();
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Login and get tokens")
    public Response login(@Valid AuthDto.LoginRequest request) {
        AuthDto.TokenResponse tokens = authService.login(request);
        return Response.ok(ApiResponse.success(tokens)).build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Refresh access token")
    public Response refresh(@QueryParam("refreshToken") String refreshToken) {
        AuthDto.TokenResponse tokens = authService.refreshToken(refreshToken);
        return Response.ok(ApiResponse.success(tokens)).build();
    }

    @GET
    @Path("/me")
    @Operation(summary = "Get current user profile")
    public Response getProfile() {
        String keycloakId = jwt.getSubject();
        AuthDto.UserProfileResponse profile = authService.getProfile(keycloakId);
        return Response.ok(ApiResponse.success(profile)).build();
    }

    @PUT
    @Path("/me")
    @Operation(summary = "Update current user profile")
    public Response updateProfile(@Valid AuthDto.RegisterRequest request) {
        String keycloakId = jwt.getSubject();
        AuthDto.UserProfileResponse profile = authService.updateProfile(keycloakId, request);
        return Response.ok(ApiResponse.success("Profile updated", profile)).build();
    }
}
