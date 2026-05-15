package com.sportify.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingDto {

    // ── Create Booking Request ────────────────────────────────────────────────

    @Data
    public static class CreateBookingRequest {
        @NotNull(message = "fieldId is required")
        public Long fieldId;

        @NotNull(message = "bookingDate is required")
        @FutureOrPresent(message = "Booking date must be today or in the future")
        public LocalDate bookingDate;

        @NotNull(message = "startTime is required")
        public LocalTime startTime;

        @NotNull(message = "endTime is required")
        public LocalTime endTime;

        /** Ghi chú tuỳ chọn của khách hàng */
        public String note;
    }

    // ── Booking Response ──────────────────────────────────────────────────────

    @Data
    public static class BookingResponse {
        public Long          id;
        public Long          userId;
        public Long          fieldId;
        public String        fieldName;
        public String        locationName;
        public LocalDate     bookingDate;
        public LocalTime     startTime;
        public LocalTime     endTime;
        public double        durationHours;
        public BigDecimal    totalPrice;
        public String        status;
        public String        note;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    // ── Admin: List All Bookings (filter) ─────────────────────────────────────

    @Data
    public static class BookingFilter {
        public Long      fieldId;
        public Long      userId;
        public LocalDate dateFrom;
        public LocalDate dateTo;
        public String    status; // PENDING | CONFIRMED | COMPLETED | CANCELLED
    }

    // ── Internal DTO dùng bởi payment-service ────────────────────────────────

    @Data
    public static class BookingInternalResponse {
        public Long       id;
        public Long       userId;
        public Long       fieldId;
        public String     fieldName;
        public BigDecimal totalPrice;
        public String     status;
    }
}
