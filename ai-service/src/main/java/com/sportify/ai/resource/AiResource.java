package com.sportify.ai.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.ai.dto.ChatDto;
import com.sportify.ai.service.ChatService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/ai")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "AI Chatbot", description = "Chatbot AI tư vấn đặt sân thể thao thông minh")
public class AiResource {

    @Inject
    ChatService chatService;

    /**
     * POST /api/v1/ai/chat
     * Gửi tin nhắn tới AI Chatbot.
     * Nếu không gửi sessionId, sẽ tạo phiên hội thoại mới.
     */
    @POST
    @Path("/chat")
    @PermitAll
    @Operation(summary = "Gửi tin nhắn tới AI Chatbot tư vấn đặt sân")
    public Response chat(ChatDto.ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Response.status(400)
                    .entity(ApiResponse.error("Message is required"))
                    .build();
        }
        ChatDto.ChatResponse chatResponse = chatService.chat(request.getMessage(), request.getSessionId());
        return Response.ok(ApiResponse.success(chatResponse)).build();
    }

    /**
     * GET /api/v1/ai/chat/{sessionId}/history
     * Lấy lịch sử hội thoại của một phiên chat.
     */
    @GET
    @Path("/chat/{sessionId}/history")
    @PermitAll
    @Operation(summary = "Lấy lịch sử hội thoại")
    public Response getHistory(@PathParam("sessionId") String sessionId) {
        ChatDto.ChatHistoryResponse history = chatService.getHistory(sessionId);
        return Response.ok(ApiResponse.success(history)).build();
    }

    /**
     * DELETE /api/v1/ai/chat/{sessionId}
     * Xóa phiên hội thoại.
     */
    @DELETE
    @Path("/chat/{sessionId}")
    @PermitAll
    @Operation(summary = "Xóa phiên hội thoại")
    public Response clearSession(@PathParam("sessionId") String sessionId) {
        chatService.clearSession(sessionId);
        return Response.ok(ApiResponse.success("Session cleared successfully", null)).build();
    }
}
