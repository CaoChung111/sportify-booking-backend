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

import java.util.function.Function;
import java.util.function.Supplier;

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
        return forward(authClient::register, body);
    }

    @POST
    @Path("/login")
    @PermitAll
    @Operation(summary = "Đăng nhập — nhận JWT Access Token")
    public Response login(Object body) {
        return forward(authClient::login, body);
    }

    @POST
    @Path("/refresh")
    @PermitAll
    @Operation(summary = "Làm mới Access Token bằng Refresh Token")
    public Response refresh(Object body) {
        return forward(authClient::refresh, body);
    }

    @GET
    @Path("/me")
    @Operation(summary = "Lấy thông tin profile của user đang đăng nhập")
    public Response getProfile(@Context HttpHeaders headers) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return forward(() -> authClient.getProfile(auth));
    }

    @PUT
    @Path("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân (fullName, phone)")
    public Response updateProfile(@Context HttpHeaders headers, Object body) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return forward(() -> authClient.updateProfile(auth, body));
    }

    @GET
    @Path("/admin/users")
    @Operation(summary = "Xem danh sách người dùng (Admin)")
    public Response getUsers(@Context HttpHeaders headers,
                             @QueryParam("keyword") String keyword,
                             @QueryParam("page") Integer page,
                             @QueryParam("size") Integer size) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return forward(() -> authClient.getUsers(auth, keyword, page, size));
    }

    @PATCH
    @Path("/admin/users/{id}/status")
    @Operation(summary = "Khóa hoặc kích hoạt tài khoản người dùng (Admin)")
    public Response updateUserStatus(@PathParam("id") Long id,
                                     @Context HttpHeaders headers,
                                     Object body) {
        String auth = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        return forward(() -> authClient.updateUserStatus(auth, id, body));
    }

    private Response forward(Supplier<Response> request) {
        try {
            return request.get();
        } catch (WebApplicationException ex) {
            return copyErrorResponse(ex.getResponse());
        }
    }

    private Response forward(Function<Object, Response> request, Object body) {
        try {
            return request.apply(body);
        } catch (WebApplicationException ex) {
            return copyErrorResponse(ex.getResponse());
        }
    }

    private Response copyErrorResponse(Response errorResponse) {
        if (errorResponse == null) {
            return Response.serverError().build();
        }

        String body = errorResponse.hasEntity() ? errorResponse.readEntity(String.class) : null;
        Response.ResponseBuilder builder = Response.status(errorResponse.getStatus());
        if (body != null && !body.isBlank()) {
            builder.entity(body).type(MediaType.APPLICATION_JSON);
        }
        return builder.build();
    }
}
