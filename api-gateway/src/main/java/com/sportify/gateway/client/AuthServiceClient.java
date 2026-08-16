package com.sportify.gateway.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "auth-service")
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AuthServiceClient {

    @POST
    @Path("/register")
    Response register(Object body);

    @POST
    @Path("/login")
    Response login(Object body);

    @POST
    @Path("/refresh")
    Response refresh(Object body);

    @GET
    @Path("/me")
    Response getProfile(@HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/me")
    Response updateProfile(@HeaderParam("Authorization") String authorization, Object body);

    // ── Admin User Management (UC16) ─────────────────────────────────────────

    @GET
    @Path("/admin/users")
    Response getUsers(@HeaderParam("Authorization") String authorization,
                      @QueryParam("keyword") String keyword,
                      @QueryParam("page") Integer page,
                      @QueryParam("size") Integer size);

    @PATCH
    @Path("/admin/users/{id}/status")
    Response updateUserStatus(@HeaderParam("Authorization") String authorization,
                              @PathParam("id") Long id,
                              Object body);
}
