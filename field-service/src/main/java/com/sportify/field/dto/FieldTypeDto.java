package com.sportify.field.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class FieldTypeDto {

    // ── Create / Update ───────────────────────────────────────────────────────

    @Data
    public static class CreateFieldTypeRequest {

        @NotNull(message = "sportId is required")
        public Long sportId;

        @NotBlank(message = "FieldType name is required")
        @Size(max = 100, message = "FieldType name must not exceed 100 characters")
        public String name;

        @NotNull(message = "playerCapacity is required")
        @Min(value = 1, message = "playerCapacity must be at least 1")
        public Integer playerCapacity;
    }

    // ── Response ──────────────────────────────────────────────────────────────

    @Data
    public static class FieldTypeResponse {
        public Long    id;
        public String  name;
        public Integer playerCapacity;

        // Sport info
        public Long   sportId;
        public String sportName;
        public String sportSlug;
    }
}
