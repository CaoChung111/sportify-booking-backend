package com.sportify.gateway.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ai-service")
@Path("/api/v1/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AiServiceClient {

    @POST
    @Path("/chat")
    Response chat(Object body);

    @GET
    @Path("/chat/{sessionId}/history")
    Response getHistory(@PathParam("sessionId") String sessionId);

    @DELETE
    @Path("/chat/{sessionId}")
    Response clearSession(@PathParam("sessionId") String sessionId);
}
