package com.sportify.ai.dto;

import lombok.Data;
import java.util.List;

public class ChatDto {

    @Data
    public static class ChatRequest {
        /**
         * Tin nhắn người dùng gửi tới chatbot
         */
        public String message;

        /**
         * ID phiên hội thoại. Nếu null/empty thì tạo phiên mới.
         */
        public String sessionId;
    }

    @Data
    public static class ChatResponse {
        /**
         * ID phiên hội thoại (dùng để gửi tiếp tin nhắn trong cùng phiên)
         */
        public String sessionId;

        /**
         * Câu trả lời từ AI
         */
        public String reply;

        /**
         * Danh sách gợi ý câu hỏi tiếp theo
         */
        public List<String> suggestions;
    }

    @Data
    public static class ChatMessage {
        public String role; // "user" or "model"
        public String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class ChatHistoryResponse {
        public String sessionId;
        public List<ChatMessage> messages;
    }
}
