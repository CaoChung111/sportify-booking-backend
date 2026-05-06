package com.sportify.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingDto {

    @Data
    public static class CreateBookingRequest {
        @NotNull
        public Long fieldId;

        @NotNull
        @Future
        public LocalDate bookingDate;

        @NotNull
        public LocalTime startTime;

        @NotNull
        public LocalTime endTime;
    }

    @Data
    public static class BookingResponse {
        public Long id;
        public Long userId;
        public Long fieldId;
        public String fieldName;
        public String locationName;
        public LocalDate bookingDate;
        public LocalTime startTime;
        public LocalTime endTime;
        public BigDecimal totalPrice;
        public String status;
        public LocalDateTime createdAt;
    }
}
