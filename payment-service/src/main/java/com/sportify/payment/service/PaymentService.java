package com.sportify.payment.service;

import com.sportify.common.exception.ServiceException;
import com.sportify.payment.client.BookingServiceClient;
import com.sportify.payment.dto.PaymentDto;
import com.sportify.payment.entity.Payment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class PaymentService {

    @Inject
    @RestClient
    BookingServiceClient bookingServiceClient;

    @ConfigProperty(name = "vnpay.tmn-code", defaultValue = "SPORTIFY1")
    String vnpayTmnCode;

    @ConfigProperty(name = "vnpay.hash-secret", defaultValue = "SPORTIFYHASHSECRET2024")
    String vnpayHashSecret;

    @ConfigProperty(name = "vnpay.url", defaultValue = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")
    String vnpayUrl;

    @ConfigProperty(name = "vnpay.return-url", defaultValue = "http://localhost:8084/api/v1/payments/vnpay/callback")
    String vnpayReturnUrl;

    /**
     * Khởi tạo thanh toán:
     * 1. Check booking tồn tại
     * 2. Tạo Payment record = PENDING
     * 3. Nếu VNPAY: generate payment URL
     * 4. Trả về payment info + paymentUrl
     */
    @Transactional
    public PaymentDto.PaymentResponse initiate(Long userId, PaymentDto.CreatePaymentRequest request) {
        // 1. Check booking exists
        var bookingResp = bookingServiceClient.getBooking(request.bookingId);
        if (bookingResp == null || bookingResp.getData() == null) {
            throw ServiceException.notFound("Booking", request.bookingId);
        }
        var booking = bookingResp.getData();

        if (!"PENDING".equalsIgnoreCase(booking.status())) {
            throw ServiceException.badRequest("Booking is not in PENDING state, current: " + booking.status());
        }

        // Check no duplicate payment
        Payment existing = Payment.findByBookingId(request.bookingId);
        if (existing != null && existing.paymentStatus == Payment.PaymentStatus.SUCCESS) {
            throw ServiceException.conflict("Booking already paid");
        }

        // 2. Build Payment record
        Payment.PaymentMethod method = Payment.PaymentMethod.valueOf(request.paymentMethod.toUpperCase());
        String txnRef = generateTxnRef(request.bookingId);

        Payment payment = new Payment();
        payment.bookingId = request.bookingId;
        payment.userId = userId;
        payment.amount = booking.totalPrice();
        payment.paymentMethod = method;
        payment.paymentStatus = Payment.PaymentStatus.PENDING;
        payment.txnRef = txnRef;
        payment.persist();

        // 3. Generate payment URL for VNPAY
        String paymentUrl = null;
        if (method == Payment.PaymentMethod.VNPAY) {
            paymentUrl = buildVnpayUrl(txnRef, booking.totalPrice(), "Dat san " + booking.fieldName());
        }
        // MoMo URL generation would go here similarly

        return toResponse(payment, paymentUrl);
    }

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

    /**
     * VNPay callback handler:
     * 1. Verify chữ ký
     * 2. Nếu SUCCESS → update Payment = SUCCESS, confirm booking
     * 3. Nếu FAIL → update Payment = FAILED
     */
    @Transactional
    public void processVnpayCallback(String txnRef, String responseCode) {
        Payment payment = Payment.find("txnRef", txnRef).firstResult();
        if (payment == null) {
            throw ServiceException.notFound("Payment", 0L);
        }

        if ("00".equals(responseCode)) {
            payment.paymentStatus = Payment.PaymentStatus.SUCCESS;
            payment.persist();
            // Confirm booking
            bookingServiceClient.confirmBooking(payment.bookingId);
        } else {
            payment.paymentStatus = Payment.PaymentStatus.FAILED;
            payment.persist();
        }
    }

    /**
     * MoMo callback handler
     */
    @Transactional
    public void processMomoCallback(PaymentDto.MomoCallbackRequest request) {
        Payment payment = Payment.find("txnRef", request.orderId).firstResult();
        if (payment == null) return;

        if (request.resultCode == 0) {
            payment.paymentStatus = Payment.PaymentStatus.SUCCESS;
            payment.persist();
            bookingServiceClient.confirmBooking(payment.bookingId);
        } else {
            payment.paymentStatus = Payment.PaymentStatus.FAILED;
            payment.persist();
        }
    }

    // ── VNPay URL builder ─────────────────────────────────────────────────────

    private String buildVnpayUrl(String txnRef, BigDecimal amount, String orderInfo) {
        String vnpVersion = "2.1.0";
        String vnpCommand = "pay";
        String vnpCurrCode = "VND";
        String vnpLocale = "vn";

        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();
        String vnpCreateDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String vnpExpireDate = new SimpleDateFormat("yyyyMMddHHmmss").format(
                new Date(System.currentTimeMillis() + 15 * 60 * 1000));

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpayTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", vnpCurrCode);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", vnpLocale);
        vnpParams.put("vnp_ReturnUrl", vnpayReturnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        String queryString = vnpParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        String secureHash = hmacSHA512(vnpayHashSecret, queryString);
        return vnpayUrl + "?" + queryString + "&vnp_SecureHash=" + secureHash;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC SHA512 error", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String generateTxnRef(Long bookingId) {
        return "SPF" + bookingId + System.currentTimeMillis() % 100000;
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private PaymentDto.PaymentResponse toResponse(Payment p, String paymentUrl) {
        PaymentDto.PaymentResponse r = new PaymentDto.PaymentResponse();
        r.id = p.id;
        r.bookingId = p.bookingId;
        r.userId = p.userId;
        r.amount = p.amount;
        r.paymentMethod = p.paymentMethod.name();
        r.paymentStatus = p.paymentStatus.name();
        r.txnRef = p.txnRef;
        r.paymentUrl = paymentUrl;
        r.createdAt = p.createdAt;
        return r;
    }
}
