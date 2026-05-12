package com.sportify.field.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class LocationDto {

    // ── Create / Update ───────────────────────────────────────────────────────

    @Data
    public static class CreateLocationRequest {
        @NotBlank(message = "Location name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        public String name;

        @NotBlank(message = "Address is required")
        public String address;

        @NotBlank(message = "Region is required")
        @Size(max = 50, message = "Region must not exceed 50 characters")
        public String region;

        @Pattern(regexp = "^(\\+84|0)[0-9]{9,10}$", message = "Invalid Vietnamese phone number")
        public String hotline;
    }

    // ── Response ──────────────────────────────────────────────────────────────

    @Data
    public static class LocationResponse {
        public Long   id;
        public String name;
        public String address;
        public String region;
        public String hotline;
        public int    totalFields;
    }
}
