package com.sportify.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDto {

    @Data
    public static class CreatePaymentRequest {
        @NotNull
        public Long bookingId;

        @NotBlank
        public String paymentMethod; // CASH, VNPAY, MOMO
    }

    @Data
    public static class PaymentResponse {
        public Long id;
        public Long bookingId;
        public Long userId;
        public BigDecimal amount;
        public String paymentMethod;
        public String paymentStatus;
        public String txnRef;
        public String paymentUrl; // URL redirect cho VNPay/MoMo
        public LocalDateTime createdAt;
    }

    @Data
    public static class MomoCallbackRequest {
        public String orderId;
        public String requestId;
        public int resultCode;
        public String message;
        public String transId;
    }

    // ── Internal DTO from booking-service ────────────────────────────────────
    public record BookingDetail(
            Long id,
            Long userId,
            Long fieldId,
            String fieldName,
            BigDecimal totalPrice,
            String status
    ) {}
}
