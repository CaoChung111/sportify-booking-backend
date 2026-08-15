package com.sportify.field.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

public class FieldDto {

    // ── Create / Update Field ─────────────────────────────────────────────────

    @Data
    public static class CreateFieldRequest {
        @NotNull(message = "locationId is required")
        public Long locationId;

        @NotNull(message = "fieldTypeId is required")
        public Long fieldTypeId;

        @NotBlank(message = "Field name is required")
        @Size(max = 50, message = "Field name must not exceed 50 characters")
        public String name;

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        public String imageUrl;

        public String description;
    }

    @Data
    public static class PageResponse<T> {
        public java.util.List<T> items;
        public int page;
        public int size;
        public long totalItems;
        public int totalPages;
        public String sortBy;
        public String sortDir;
    }

    // ── Field Response ────────────────────────────────────────────────────────

    @Data
    public static class FieldResponse {
        public Long id;
        public String name;
        public String status;
        public String imageUrl;
        public String description;

        // Location info
        public Long   locationId;
        public String locationName;
        public String locationAddress;
        public String locationRegion;
        public String locationHotline;

        // FieldType info
        public Long   fieldTypeId;
        public String fieldTypeName;
        public Integer playerCapacity;

        // Sport info
        public Long   sportId;
        public String sportName;
        public String sportSlug;
    }

    // ── Price Response (trả về sau khi tính giá) ──────────────────────────────

    @Data
    public static class PriceResponse {
        public Long       fieldId;
        public String     fieldName;
        public BigDecimal totalPrice;
        public BigDecimal pricePerHour;
        public double     durationHours;
        public String     currency;
        public String     dayType;
    }
}
