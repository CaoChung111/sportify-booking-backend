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

        // 1. Handle Greetings / Small talk statically to save tokens
        if (isGreetingOrSmallTalk(message)) {
            String greetingReply = "Xin chào! Tôi là trợ lý ảo Sportify AI. Tôi có thể giúp gì cho bạn hôm nay? Bạn có thể hỏi tôi về lịch sân trống, bảng giá thuê sân, hoặc các quy định đặt/hủy sân tại Sportify.";
            
            // Save to history
            history.add(new ChatDto.ChatMessage("user", message));
            history.add(new ChatDto.ChatMessage("model", greetingReply));

            ChatDto.ChatResponse chatResponse = new ChatDto.ChatResponse();
            chatResponse.setSessionId(sessionId);
            chatResponse.setReply(greetingReply);
            chatResponse.setSuggestions(List.of("Tìm sân bóng đá trống", "Quy trình đặt sân", "Bảng giá thuê sân"));
            return chatResponse;
        }

        try {
            // Search vector store for context
            List<VectorStoreService.SearchResult> searchResults = vectorStoreService.search(message, 3);

            // 2. Intent filtering & Semantic Cache (Check scores before calling Gemini)
            if (searchResults != null && !searchResults.isEmpty()) {
                double topScore = searchResults.get(0).score();
                LOG.infof("RAG search top score: %.4f for query: %s", topScore, message);

                // Direct FAQ Match (High similarity score >= 0.82)
                if (topScore >= 0.82) {
                    String directAnswer = searchResults.get(0).document().getContent();
                    LOG.infof("Direct FAQ match found (score: %.4f). Returning static answer without calling Gemini.", topScore);

                    // Save to history
                    history.add(new ChatDto.ChatMessage("user", message));
                    history.add(new ChatDto.ChatMessage("model", directAnswer));

                    ChatDto.ChatResponse chatResponse = new ChatDto.ChatResponse();
                    chatResponse.setSessionId(sessionId);
                    chatResponse.setReply(directAnswer);
                    chatResponse.setSuggestions(generateSuggestions(directAnswer));
                    return chatResponse;
                }

                // Out of Scope Check (Very low similarity score < 0.52)
                if (topScore < 0.52) {
                    String outOfScopeMsg = "Xin lỗi, câu hỏi của bạn nằm ngoài phạm vi hỗ trợ của trợ lý ảo Sportify. Tôi chỉ có thể tư vấn các thông tin liên quan đến đặt sân thể thao, giá cả, lịch trống và chính sách của Sportify.";
                    LOG.infof("Query is out of scope (score: %.4f). Returning fixed response.", topScore);

                    ChatDto.ChatResponse chatResponse = new ChatDto.ChatResponse();
                    chatResponse.setSessionId(sessionId);
                    chatResponse.setReply(outOfScopeMsg);
                    chatResponse.setSuggestions(List.of("Chính sách hủy đặt sân", "Quy trình đặt sân", "Chính sách giá"));
                    return chatResponse;
                }
            } else {
                // If searchResults is empty, it means no keyword matches at all in fallback search
                String outOfScopeMsg = "Xin lỗi, câu hỏi của bạn nằm ngoài phạm vi hỗ trợ của trợ lý ảo Sportify. Tôi chỉ có thể tư vấn các thông tin liên quan đến đặt sân thể thao, giá cả, lịch trống và chính sách của Sportify.";
                LOG.infof("Query is out of scope (empty search results). Returning fixed response.");

                ChatDto.ChatResponse chatResponse = new ChatDto.ChatResponse();
                chatResponse.setSessionId(sessionId);
                chatResponse.setReply(outOfScopeMsg);
                chatResponse.setSuggestions(List.of("Chính sách hủy đặt sân", "Quy trình đặt sân", "Chính sách giá"));
                return chatResponse;
            }

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
            while (response.hasFunctionCalls() && iteration < 6) {
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
                    functionResponseData.put("response", functionResult);

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
        return List.of("Chính sách đặt sân", "Giá thuê sân thế nào?", "Hướng dẫn đặt sân");
    }

    private boolean isGreetingOrSmallTalk(String message) {
        if (message == null) return false;
        String msg = message.toLowerCase().trim()
                .replaceAll("[?,.!~*#]", ""); // remove punctuation
        return msg.equals("hi") || msg.equals("hello") || msg.equals("chao") || msg.equals("chào") 
            || msg.equals("chào bạn") || msg.equals("chao ban") || msg.equals("alo") || msg.equals("bắt đầu")
            || msg.equals("bat dau") || msg.equals("xin chào") || msg.equals("xin chao") || msg.equals("gud")
            || msg.equals("tạm biệt") || msg.equals("tam biet") || msg.equals("bye") || msg.equals("ok") || msg.equals("oke");
    }
}
