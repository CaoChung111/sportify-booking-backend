package com.sportify.field.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

public class PriceDto {

    // ── Create / Update ───────────────────────────────────────────────────────

    @Data
    public static class CreatePriceRequest {
        @NotNull(message = "locationId is required")
        public Long locationId;

        @NotNull(message = "fieldTypeId is required")
        public Long fieldTypeId;

        /** Giờ bắt đầu khung giá, format: HH:mm (VD: "06:00") */
        @NotBlank(message = "startTime is required")
        public String startTime;

        /** Giờ kết thúc khung giá, format: HH:mm (VD: "11:00") */
        @NotBlank(message = "endTime is required")
        public String endTime;

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        public BigDecimal price;

        /**
         * Loại ngày áp dụng: WEEKDAY | WEEKEND | HOLIDAY
         */
        @NotBlank(message = "dayType is required (WEEKDAY, WEEKEND, or HOLIDAY)")
        public String dayType;
    }

    // ── Response ──────────────────────────────────────────────────────────────

    @Data
    public static class PriceRuleResponse {
        public Long       id;
        public Long       locationId;
        public String     locationName;
        public Long       fieldTypeId;
        public String     fieldTypeName;
        public String     startTime;
        public String     endTime;
        public BigDecimal price;
        public String     currency;
        public String     dayType;
    }
}
