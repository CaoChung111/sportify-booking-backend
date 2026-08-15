package com.sportify.field.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class SportDto {

    // ── Create ────────────────────────────────────────────────────────────────

    @Data
    public static class CreateSportRequest {
        @NotBlank(message = "Sport name is required")
        @Size(max = 50, message = "Name must not exceed 50 characters")
        public String name;

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers and hyphens")
        @Size(max = 50, message = "Slug must not exceed 50 characters")
        public String slug;
    }

    // ── Response ──────────────────────────────────────────────────────────────

    @Data
    public static class SportResponse {
        public Long   id;
        public String name;
        public String slug;
    }
}
