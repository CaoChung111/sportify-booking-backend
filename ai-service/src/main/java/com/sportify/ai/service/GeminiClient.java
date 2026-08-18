package com.sportify.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportify.common.exception.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class GeminiClient {
    private static final Logger LOG = Logger.getLogger(GeminiClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "gemini.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "gemini.model", defaultValue = "gemini-3.5-flash")
    String model;

    @ConfigProperty(name = "gemini.embedding-model", defaultValue = "gemini-embedding-001")
    String embeddingModel;

    public GeminiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public record FunctionCallInfo(String name, Map<String, Object> args, String id) {}

    public record GeminiResponse(String text, List<FunctionCallInfo> functionCalls, Map<String, Object> rawModelContent) {
        public boolean hasFunctionCalls() { return functionCalls != null && !functionCalls.isEmpty(); }
        public boolean hasText() { return text != null && !text.isBlank(); }
    }

    public GeminiResponse generateContent(List<Map<String, Object>> contents,
                                          List<Map<String, Object>> tools,
                                          Map<String, Object> systemInstruction) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }
        if (systemInstruction != null && !systemInstruction.isEmpty()) {
            requestBody.put("systemInstruction", systemInstruction);
        }

        return executeGenerateContentRequest(requestBody);
    }

    public GeminiResponse generateContentWithFunctionResponse(
            List<Map<String, Object>> contents,
            List<Map<String, Object>> tools,
            Map<String, Object> systemInstruction,
            Map<String, Object> functionCallContent,
            String functionName,
            Object functionResult) {

        List<Map<String, Object>> updatedContents = new ArrayList<>(contents);
        
        // Add the model's function call response back into the context
        updatedContents.add(functionCallContent);

        // Add the tool execution result
        Map<String, Object> functionResponsePart = new HashMap<>();
        Map<String, Object> functionResponseData = new HashMap<>();
        functionResponseData.put("name", functionName);
        functionResponseData.put("response", Map.of("content", functionResult));
        functionResponsePart.put("functionResponse", functionResponseData);

        Map<String, Object> functionResponseRole = new HashMap<>();
        functionResponseRole.put("role", "user"); // API required role for function response
        functionResponseRole.put("parts", List.of(functionResponsePart));

        updatedContents.add(functionResponseRole);

        return generateContent(updatedContents, tools, systemInstruction);
    }

    private GeminiResponse executeGenerateContentRequest(Map<String, Object> requestBody) {
        if (apiKey == null || apiKey.trim().isEmpty() || "placeholder-key".equals(apiKey)) {
            throw ServiceException.badRequest("Chưa cấu hình Google Gemini API Key. Hãy cấu hình GEMINI_API_KEY trong file .env");
        }
        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            LOG.infof("Sending request to Gemini: %s", jsonBody);
            
            HttpResponse<String> response = null;
            int retries = 3;
            long delay = 1000; // 1s base delay
            
            for (int i = 0; i < retries; i++) {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429) {
                    LOG.warnf("Gemini API Rate Limited (429). Retrying in %d ms... (Attempt %d/%d)", delay, i + 1, retries);
                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff
                } else {
                    break;
                }
            }
            
            LOG.infof("Received response from Gemini: %s", response.body());
            if (response.statusCode() != 200) {
                LOG.errorf("Gemini API Error: %d - %s", response.statusCode(), response.body());
                throw ServiceException.badRequest("Lỗi khi gọi API Gemini: " + response.statusCode());
            }

            Map<String, Object> responseBody = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            return parseGeminiResponse(responseBody);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to generate content with Gemini", e);
            throw ServiceException.badRequest("Lỗi hệ thống khi tương tác với AI: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private GeminiResponse parseGeminiResponse(Map<String, Object> responseBody) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return new GeminiResponse(null, null, null);
        }

        Map<String, Object> firstCandidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
        if (content == null) {
            return new GeminiResponse(null, null, null);
        }

        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            return new GeminiResponse(null, null, content);
        }

        String text = null;
        List<FunctionCallInfo> functionCalls = new ArrayList<>();

        for (Map<String, Object> part : parts) {
            if (part.containsKey("text")) {
                text = (String) part.get("text");
            } else if (part.containsKey("functionCall")) {
                Map<String, Object> funcCall = (Map<String, Object>) part.get("functionCall");
                String name = (String) funcCall.get("name");
                Map<String, Object> args = (Map<String, Object>) funcCall.get("args");
                String id = (String) funcCall.get("id");
                functionCalls.add(new FunctionCallInfo(name, args, id));
            }
        }

        return new GeminiResponse(text, functionCalls, content);
    }

    @SuppressWarnings("unchecked")
    public float[] embedContent(String text) {
        if (apiKey == null || apiKey.trim().isEmpty() || "placeholder-key".equals(apiKey)) {
            throw ServiceException.badRequest("Chưa cấu hình Google Gemini API Key. Hãy cấu hình GEMINI_API_KEY trong file .env");
        }
        try {
            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:embedContent?key=%s", embeddingModel, apiKey);
            Map<String, Object> requestBody = Map.of(
                    "model", "models/" + embeddingModel,
                    "content", Map.of("parts", List.of(Map.of("text", text)))
            );
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = null;
            int retries = 3;
            long delay = 1000; // 1s base delay
            
            for (int i = 0; i < retries; i++) {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429) {
                    LOG.warnf("Gemini Embedding API Rate Limited (429). Retrying in %d ms... (Attempt %d/%d)", delay, i + 1, retries);
                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff
                } else {
                    break;
                }
            }
            
            if (response.statusCode() != 200) {
                LOG.errorf("Gemini Embedding API Error: %d - %s", response.statusCode(), response.body());
                throw ServiceException.badRequest("Lỗi khi gọi API Embedding");
            }

            Map<String, Object> responseBody = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            Map<String, Object> embedding = (Map<String, Object>) responseBody.get("embedding");
            List<Double> values = (List<Double>) embedding.get("values");

            float[] floatValues = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                floatValues[i] = values.get(i).floatValue();
            }
            return floatValues;
        } catch (Exception e) {
            LOG.error("Failed to generate embedding", e);
            throw ServiceException.badRequest("Lỗi khi tạo embedding");
        }
    }
}
