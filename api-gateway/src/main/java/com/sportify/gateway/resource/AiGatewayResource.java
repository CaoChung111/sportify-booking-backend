package com.sportify.gateway.resource;

import com.sportify.gateway.client.AiServiceClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/v1/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AI Gateway", description = "Proxy → ai-service (port 8085)")
public class AiGatewayResource {

    @Inject
    @RestClient
    AiServiceClient aiClient;

    @POST
    @Path("/chat")
    @PermitAll
    @Operation(summary = "Gửi tin nhắn tới AI Chatbot")
    public Response chat(Object body) {
        return aiClient.chat(body);
    }

    @GET
    @Path("/chat/{sessionId}/history")
    @PermitAll
    @Operation(summary = "Lấy lịch sử hội thoại")
    public Response getHistory(@PathParam("sessionId") String sessionId) {
        return aiClient.getHistory(sessionId);
    }

    @DELETE
    @Path("/chat/{sessionId}")
    @PermitAll
    @Operation(summary = "Xóa phiên hội thoại")
    public Response clearSession(@PathParam("sessionId") String sessionId) {
        return aiClient.clearSession(sessionId);
    }
}
