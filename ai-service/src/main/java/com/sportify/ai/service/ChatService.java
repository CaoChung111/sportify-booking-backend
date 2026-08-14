package com.sportify.ai.service;

import com.sportify.ai.config.SystemPromptConfig;
import com.sportify.ai.config.ToolDeclarations;
import com.sportify.ai.dto.ChatDto;
import com.sportify.common.exception.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChatService {
    private static final Logger LOG = Logger.getLogger(ChatService.class);

    private final ConcurrentHashMap<String, List<ChatDto.ChatMessage>> sessions = new ConcurrentHashMap<>();

    @Inject
    GeminiClient geminiClient;

    @Inject
    VectorStoreService vectorStoreService;

    @Inject
    FunctionExecutor functionExecutor;

    @Inject
    SystemPromptConfig systemPromptConfig;

    @Inject
    ToolDeclarations toolDeclarations;

    @ConfigProperty(name = "gemini.max-history-size", defaultValue = "20")
    int maxHistorySize;

    public ChatDto.ChatResponse chat(String message, String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        List<ChatDto.ChatMessage> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        try {
            // Search vector store for context
            List<VectorStoreService.SearchResult> searchResults = vectorStoreService.search(message, 3);
            String ragContext = buildRagContext(searchResults);

            // Build system instruction
            Map<String, Object> systemInstruction = buildSystemInstruction(ragContext);

            // Build contents
            List<Map<String, Object>> contents = buildContents(history);
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", message))
            ));

            List<Map<String, Object>> tools = toolDeclarations.getToolDeclarations();

            GeminiClient.GeminiResponse response = geminiClient.generateContent(contents, tools, systemInstruction);

            int iteration = 0;
            // Handle multiple function calls (including parallel calls)
            while (response.hasFunctionCalls() && iteration < 3) {
                iteration++;
                List<GeminiClient.FunctionCallInfo> functionCalls = response.functionCalls();
                
                // Add the model's function call response back into the context
                contents.add(response.rawModelContent());

                List<Map<String, Object>> responseParts = new ArrayList<>();
                for (GeminiClient.FunctionCallInfo functionCall : functionCalls) {
                    LOG.infof("Executing function: %s (id: %s)", functionCall.name(), functionCall.id());
                    Object functionResult = functionExecutor.execute(functionCall.name(), functionCall.args());

                    Map<String, Object> functionResponseData = new HashMap<>();
                    functionResponseData.put("name", functionCall.name());
                    if (functionCall.id() != null) {
                        functionResponseData.put("id", functionCall.id());
                    }
                    functionResponseData.put("response", Map.of("content", functionResult));

                    Map<String, Object> functionResponsePart = new HashMap<>();
                    functionResponsePart.put("functionResponse", functionResponseData);
                    responseParts.add(functionResponsePart);
                }

                Map<String, Object> functionResponseRole = new HashMap<>();
                functionResponseRole.put("role", "user"); // API required role for function response
                functionResponseRole.put("parts", responseParts);

                contents.add(functionResponseRole);

                response = geminiClient.generateContent(contents, tools, systemInstruction);
            }

            if (!response.hasText()) {
                throw ServiceException.badRequest("Không nhận được phản hồi từ AI");
            }

            String replyText = response.text();

            // Save history
            history.add(new ChatDto.ChatMessage("user", message));
            history.add(new ChatDto.ChatMessage("model", replyText));

            // Trim history
            if (history.size() > maxHistorySize) {
                history = new ArrayList<>(history.subList(history.size() - maxHistorySize, history.size()));
                sessions.put(sessionId, history);
            }

            ChatDto.ChatResponse chatResponse = new ChatDto.ChatResponse();
            chatResponse.setSessionId(sessionId);
            chatResponse.setReply(replyText);
            chatResponse.setSuggestions(generateSuggestions(replyText));
            return chatResponse;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Lỗi khi xử lý tin nhắn chat", e);
            throw ServiceException.badRequest("Hệ thống AI đang bận hoặc xảy ra lỗi: " + e.getMessage());
        }
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public ChatDto.ChatHistoryResponse getHistory(String sessionId) {
        ChatDto.ChatHistoryResponse response = new ChatDto.ChatHistoryResponse();
        response.setSessionId(sessionId);
        response.setMessages(sessions.getOrDefault(sessionId, new ArrayList<>()));
        return response;
    }

    private String buildRagContext(List<VectorStoreService.SearchResult> searchResults) {
        if (searchResults == null || searchResults.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Thông tin tham khảo (RAG Context):\n");
        for (VectorStoreService.SearchResult result : searchResults) {
            sb.append("- ").append(result.document().getTitle()).append(": ")
              .append(result.document().getContent()).append("\n");
        }
        return sb.toString();
    }

    private Map<String, Object> buildSystemInstruction(String ragContext) {
        String basePrompt = systemPromptConfig.getSystemPrompt();
        String currentDateTime = "Thời gian hiện tại: " + LocalDateTime.now().toString() + "\n\n";
        String fullPrompt = currentDateTime + basePrompt + "\n\n" + ragContext;

        return Map.of(
                "parts", List.of(Map.of("text", fullPrompt))
        );
    }

    private List<Map<String, Object>> buildContents(List<ChatDto.ChatMessage> history) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatDto.ChatMessage msg : history) {
            contents.add(Map.of(
                    "role", msg.getRole(),
                    "parts", List.of(Map.of("text", msg.getContent()))
            ));
        }
        return contents;
    }

    private List<String> generateSuggestions(String reply) {
        return List.of("Tìm sân bóng gần tôi", "Giá thuê sân thế nào?", "Hướng dẫn đặt sân");
    }
}
