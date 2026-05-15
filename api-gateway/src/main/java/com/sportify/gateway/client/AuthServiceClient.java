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
}
