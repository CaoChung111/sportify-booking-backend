package com.sportify.field.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

public class FieldDto {

    @Data
    public static class CreateFieldRequest {
        @NotNull
        public Long locationId;

        @NotNull
        public Long fieldTypeId;

        @NotBlank
        public String name;
    }

    @Data
    public static class FieldResponse {
        public Long id;
        public String name;
        public String status;
        public Long locationId;
        public String locationName;
        public Long fieldTypeId;
        public String fieldTypeName;
        public String sportName;
    }

    @Data
    public static class PriceResponse {
        public Long fieldId;
        public BigDecimal totalPrice;
        public String currency;
    }
}
