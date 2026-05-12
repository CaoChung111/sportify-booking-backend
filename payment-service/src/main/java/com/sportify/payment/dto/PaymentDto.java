package com.sportify.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDto {

    // ── Khởi tạo thanh toán ───────────────────────────────────────────────────

    @Data
    public static class CreatePaymentRequest {
        @NotNull(message = "bookingId is required")
        public Long bookingId;

        /**
         * Phương thức thanh toán: CASH | VNPAY | MOMO
         * - CASH: Admin xác nhận thủ công
         * - VNPAY: Hệ thống tạo URL redirect sang VNPAY
         * - MOMO: Hệ thống tạo URL redirect sang MoMo
         */
        @NotBlank(message = "paymentMethod is required")
        @Pattern(regexp = "^(CASH|VNPAY|MOMO)$",
                 message = "paymentMethod must be CASH, VNPAY, or MOMO")
        public String paymentMethod;
    }

    // ── Kết quả thanh toán ────────────────────────────────────────────────────

    @Data
    public static class PaymentResponse {
        public Long          id;
        public Long          bookingId;
        public Long          userId;
        public BigDecimal    amount;
        public String        paymentMethod;
        public String        paymentStatus;
        public String        txnRef;
        /** URL redirect sang trang thanh toán (chỉ có với VNPAY/MoMo) */
        public String        paymentUrl;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    // ── VNPay Callback (từ VNPay gọi lại) ────────────────────────────────────

    @Data
    public static class VnpayCallbackRequest {
        public String vnpTxnRef;
        public String vnpResponseCode;
        public String vnpTransactionNo;
        public String vnpBankCode;
        public String vnpAmount;
        public String vnpSecureHash;
    }

    // ── MoMo Callback (từ MoMo gọi lại) ──────────────────────────────────────

    @Data
    public static class MomoCallbackRequest {
        public String orderId;       // txnRef của hệ thống
        public String requestId;
        public int    resultCode;    // 0 = success
        public String message;
        public String transId;       // ID giao dịch của MoMo
        public String signature;     // để verify
    }

    // ── CASH: Admin xác nhận thanh toán tiền mặt ──────────────────────────────

    @Data
    public static class CashConfirmRequest {
        @NotNull(message = "paymentId is required")
        public Long paymentId;
        public String adminNote;
    }

    // ── Internal DTO từ booking-service ───────────────────────────────────────

    public record BookingDetail(
            Long id,
            Long userId,
            Long fieldId,
            String fieldName,
            String locationName,
            BigDecimal totalPrice,
            String status
    ) {}
}
