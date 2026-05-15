package com.sportify.payment.service;

import com.sportify.common.exception.ServiceException;
import com.sportify.payment.client.BookingServiceClient;
import com.sportify.payment.dto.PaymentDto;
import com.sportify.payment.entity.Payment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PaymentService {

    private static final Charset VNPAY_CHARSET = StandardCharsets.US_ASCII;

    @Inject
    @RestClient
    BookingServiceClient bookingServiceClient;

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "vnpay.tmn-code",      defaultValue = "SPORTIFY1")
    String vnpayTmnCode;

    @ConfigProperty(name = "vnpay.hash-secret",   defaultValue = "SPORTIFYHASHSECRET2024")
    String vnpayHashSecret;

    @ConfigProperty(name = "vnpay.url",
            defaultValue = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")
    String vnpayUrl;

    @ConfigProperty(name = "vnpay.return-url",
            defaultValue = "http://localhost:8084/api/v1/payments/vnpay/callback")
    String vnpayReturnUrl;

    // ── Khởi tạo Thanh toán ───────────────────────────────────────────────────

    /**
     * Luồng khởi tạo thanh toán:
     *
     * 1. Lấy thông tin booking từ booking-service, kiểm tra status = PENDING
     * 2. Kiểm tra chống thanh toán đúp (booking đã có payment SUCCESS)
     * 3. Sinh mã giao dịch nội bộ (TxnRef)
     * 4. Tạo bản ghi Payment (PENDING)
     * 5. Nếu VNPAY: sinh Payment URL (bảo mật bằng HMAC-SHA512)
     * 6. Trả về PaymentResponse kèm paymentUrl (nếu có)
     */
    @Transactional
    public PaymentDto.PaymentResponse initiate(Long userId, PaymentDto.CreatePaymentRequest request) {
        // Bước 1: Lấy & kiểm tra booking
        var bookingResp = bookingServiceClient.getBooking(request.bookingId);
        if (bookingResp == null || bookingResp.getData() == null) {
            throw ServiceException.notFound("Booking", request.bookingId);
        }
        var booking = bookingResp.getData();

        if (!"PENDING".equalsIgnoreCase(booking.status())) {
            throw ServiceException.badRequest(
                    "Booking is not in PENDING state. Current status: " + booking.status() +
                    ". Only PENDING bookings can be paid.");
        }

        // Bước 2: Chống thanh toán đúp
        Payment existing = Payment.findByBookingId(request.bookingId);
        if (existing != null && existing.paymentStatus == Payment.PaymentStatus.SUCCESS) {
            throw ServiceException.conflict("Booking #" + request.bookingId + " has already been paid successfully");
        }
        // Nếu có payment cũ bị FAILED → cho phép tạo mới (retry payment)
        if (existing != null) {
            existing.delete();
            entityManager.flush();
        }

        // Bước 3: Sinh TxnRef duy nhất
        Payment.PaymentMethod method = parseMethod(request.paymentMethod);
        String txnRef = generateTxnRef(request.bookingId);

        // Bước 4: Tạo Payment record
        Payment payment       = new Payment();
        payment.bookingId     = request.bookingId;
        payment.userId        = userId;
        payment.amount        = booking.totalPrice();
        payment.paymentMethod = method;
        payment.paymentStatus = Payment.PaymentStatus.PENDING;
        payment.txnRef        = txnRef;
        payment.persist();

        // Bước 5: Tạo Payment URL (chỉ với VNPAY)
        String paymentUrl = null;
        switch (method) {
            case VNPAY -> paymentUrl = buildVnpayUrl(
                    txnRef, booking.totalPrice(),
                    "Sportify booking " + request.bookingId);
            case CASH  -> { /* Cash: admin xác nhận thủ công, không có URL */ }
        }

        return toResponse(payment, paymentUrl);
    }

    // ── Lấy Lịch Sử Thanh Toán ───────────────────────────────────────────────

    public List<PaymentDto.PaymentResponse> getByUserId(Long userId) {
        return Payment.findByUserId(userId).stream()
                .map(p -> toResponse(p, null))
                .collect(Collectors.toList());
    }

    public PaymentDto.PaymentResponse getById(Long id) {
        Payment payment = Payment.findById(id);
        if (payment == null) throw ServiceException.notFound("Payment", id);
        return toResponse(payment, null);
    }

    public PaymentDto.PaymentResponse getByBookingId(Long bookingId) {
        Payment payment = Payment.findByBookingId(bookingId);
        if (payment == null) throw ServiceException.notFound("Payment for booking", bookingId);
        return toResponse(payment, null);
    }

    // ── VNPay Callback Handler ────────────────────────────────────────────────

    /**
     * Xử lý callback từ VNPay:
     *
     * 1. Tìm Payment theo txnRef
     * 2. Verify chữ ký HMAC-SHA512 (tránh giả mạo callback)
     * 3. Nếu responseCode = "00" (thành công):
     *    - Cập nhật Payment = SUCCESS
     *    - Gọi booking-service xác nhận đơn (Booking = CONFIRMED)
     * 4. Nếu thất bại:
     *    - Cập nhật Payment = FAILED (cho phép retry)
     */
    @Transactional
    public String processVnpayCallback(String txnRef, String responseCode, String secureHash,
                                        Map<String, String> allParams) {
        Payment payment = Payment.find("txnRef", txnRef).firstResult();
        if (payment == null) {
            throw ServiceException.notFound("Payment with txnRef", 0L);
        }

        // Idempotency: đã xử lý rồi → bỏ qua
        if (payment.paymentStatus == Payment.PaymentStatus.SUCCESS) {
            try {
                bookingServiceClient.markBookingPaid(payment.bookingId);
            } catch (Exception e) {
                // Keep callback idempotent; a later retry can reconcile booking status.
            }
            return "already_processed";
        }

        // Verify chữ ký
        boolean validSignature = verifyVnpaySignature(allParams, secureHash);
        if (!validSignature) {
            payment.paymentStatus = Payment.PaymentStatus.FAILED;
            payment.persist();
            throw ServiceException.badRequest("Invalid VNPAY signature — possible fraud attempt");
        }

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            payment.paymentStatus = Payment.PaymentStatus.SUCCESS;
            payment.persist();

            // Gọi booking-service xác nhận đơn
            try {
                bookingServiceClient.markBookingPaid(payment.bookingId);
            } catch (Exception e) {
                // Log nhưng không rollback payment — booking-service có thể retry
                // Trong production: dùng message queue (Kafka) để đảm bảo at-least-once
            }
            return "success";
        } else {
            payment.paymentStatus = Payment.PaymentStatus.FAILED;
            payment.persist();
            return "failed";
        }
    }

    // ── Cash: Admin xác nhận thanh toán tiền mặt ─────────────────────────────

    /**
     * Admin xác nhận khách đã trả tiền mặt.
     * Cập nhật Payment = SUCCESS → Booking = CONFIRMED.
     */
    @Transactional
    public PaymentDto.PaymentResponse confirmCash(Long paymentId) {
        Payment payment = Payment.findById(paymentId);
        if (payment == null) throw ServiceException.notFound("Payment", paymentId);

        if (payment.paymentMethod != Payment.PaymentMethod.CASH) {
            throw ServiceException.badRequest("Only CASH payments can be confirmed manually");
        }
        if (payment.paymentStatus == Payment.PaymentStatus.SUCCESS) {
            throw ServiceException.conflict("Payment is already confirmed");
        }

        payment.paymentStatus = Payment.PaymentStatus.SUCCESS;
        payment.persist();

        bookingServiceClient.confirmBooking(payment.bookingId);

        return toResponse(payment, null);
    }

    // ── VNPay URL Builder ─────────────────────────────────────────────────────

    @Transactional
    public PaymentDto.PaymentResponse markCashSuccessByBookingId(Long bookingId) {
        Payment payment = Payment.findByBookingId(bookingId);
        if (payment == null) throw ServiceException.notFound("Payment for booking", bookingId);

        if (payment.paymentMethod != Payment.PaymentMethod.CASH) {
            return toResponse(payment, null);
        }

        if (payment.paymentStatus != Payment.PaymentStatus.SUCCESS) {
            payment.paymentStatus = Payment.PaymentStatus.SUCCESS;
            payment.persist();
        }

        return toResponse(payment, null);
    }

    private String buildVnpayUrl(String txnRef, BigDecimal amount, String orderInfo) {
        long   vnpAmount     = amount.multiply(BigDecimal.valueOf(100)).longValue();
        String vnpCreateDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String vnpExpireDate = new SimpleDateFormat("yyyyMMddHHmmss").format(
                new Date(System.currentTimeMillis() + 15 * 60 * 1000L)); // 15 phút

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version",    "2.1.0");
        vnpParams.put("vnp_Command",    "pay");
        vnpParams.put("vnp_TmnCode",    vnpayTmnCode);
        vnpParams.put("vnp_Amount",     String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode",   "VND");
        vnpParams.put("vnp_TxnRef",     txnRef);
        vnpParams.put("vnp_OrderInfo",  orderInfo);
        vnpParams.put("vnp_OrderType",  "other");
        vnpParams.put("vnp_Locale",     "vn");
        vnpParams.put("vnp_ReturnUrl",  vnpayReturnUrl);
        vnpParams.put("vnp_IpAddr",     "127.0.0.1");
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Tạo chuỗi query và ký bằng HMAC-SHA512
        String queryString = vnpParams.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> encodeVnpay(e.getKey()) + "=" + encodeVnpay(e.getValue()))
                .collect(Collectors.joining("&"));

        String secureHash = hmacSHA512(vnpayHashSecret, queryString);
        return vnpayUrl + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Verify chữ ký VNPay từ callback.
     * Loại bỏ vnp_SecureHash và vnp_SecureHashType khỏi params → tính lại hash → so sánh.
     */
    private boolean verifyVnpaySignature(Map<String, String> params, String receivedHash) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        String dataToSign = sortedParams.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> encodeVnpay(e.getKey()) + "=" + encodeVnpay(e.getValue()))
                .collect(Collectors.joining("&"));

        String calculatedHash = hmacSHA512(vnpayHashSecret, dataToSign);
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                // FIX LỖI Ở ĐÂY: Bắt buộc phải có & 0xff để convert chuẩn byte sang hex
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA512 signing error", e);
        }
    }

    private String encodeVnpay(String value) {
        return URLEncoder.encode(value, VNPAY_CHARSET);
    }
    /**
     * Sinh mã giao dịch nội bộ duy nhất.
     * Format: SPF{bookingId}{timestamp5digits}
     */
    private String generateTxnRef(Long bookingId) {
        return "SPF" + bookingId + (System.currentTimeMillis() % 100000);
    }

    private Payment.PaymentMethod parseMethod(String method) {
        try {
            return Payment.PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ServiceException.badRequest("Invalid payment method: '" + method + "'. Must be CASH or VNPAY");
        }
    }

    private PaymentDto.PaymentResponse toResponse(Payment p, String paymentUrl) {
        PaymentDto.PaymentResponse r = new PaymentDto.PaymentResponse();
        r.id            = p.id;
        r.bookingId     = p.bookingId;
        r.userId        = p.userId;
        r.amount        = p.amount;
        r.paymentMethod = p.paymentMethod.name();
        r.paymentStatus = p.paymentStatus.name();
        r.txnRef        = p.txnRef;
        r.paymentUrl    = paymentUrl;
        r.createdAt     = p.createdAt;
        r.updatedAt     = p.updatedAt;
        return r;
    }
}
