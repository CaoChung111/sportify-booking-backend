package com.sportify.payment.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.payment.dto.PaymentDto;
import com.sportify.payment.service.PaymentService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments", description = "Thanh toán và xử lý giao dịch (VNPAY, Tiền mặt)")
public class PaymentResource {

    @Inject PaymentService paymentService;
    @Inject JsonWebToken   jwt;

    private Long currentUserId() {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) return Long.valueOf(userIdClaim.toString());
        } catch (Exception ignored) {}
        return 1L; // dev fallback
    }

    // ── Khởi tạo Thanh toán ──────────────────────────────────────────────────

    /**
     * POST /api/v1/payments
     * Khởi tạo thanh toán cho một booking đang PENDING.
     * - VNPAY: trả về paymentUrl để redirect khách hàng
     * - CASH: chờ admin xác nhận thủ công
     */
    @POST
    @Operation(summary = "Khởi tạo thanh toán cho booking (VNPAY | CASH)")
    @APIResponse(responseCode = "201", description = "Thanh toán được khởi tạo, trả về paymentUrl")
    @APIResponse(responseCode = "400", description = "Booking không ở trạng thái PENDING")
    @APIResponse(responseCode = "409", description = "Booking đã được thanh toán")
    public Response initiate(@Valid PaymentDto.CreatePaymentRequest request) {
        PaymentDto.PaymentResponse payment = paymentService.initiate(currentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Payment initiated", payment))
                .build();
    }

    // ── Lịch sử thanh toán ────────────────────────────────────────────────────

    @GET
    @Operation(summary = "Lấy lịch sử thanh toán của user đang đăng nhập")
    public Response getMyPayments() {
        List<PaymentDto.PaymentResponse> payments = paymentService.getByUserId(currentUserId());
        return Response.ok(ApiResponse.success(payments)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Lấy chi tiết thanh toán theo ID")
    public Response getById(@PathParam("id") Long id) {
        PaymentDto.PaymentResponse payment = paymentService.getById(id);
        return Response.ok(ApiResponse.success(payment)).build();
    }

    @GET
    @Path("/booking/{bookingId}")
    @Operation(summary = "Lấy thông tin thanh toán theo Booking ID")
    public Response getByBookingId(@PathParam("bookingId") Long bookingId) {
        PaymentDto.PaymentResponse payment = paymentService.getByBookingId(bookingId);
        return Response.ok(ApiResponse.success(payment)).build();
    }

    // ── CASH: Admin xác nhận tiền mặt ────────────────────────────────────────

    /**
     * PATCH /api/v1/payments/{id}/confirm-cash
     * Admin xác nhận khách đã trả tiền mặt tại quầy.
     * Tự động cập nhật Payment = SUCCESS và Booking = CONFIRMED.
     */
    @PATCH
    @Path("/{id}/confirm-cash")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Admin xác nhận thanh toán tiền mặt (CASH)")
    @APIResponse(responseCode = "200", description = "Xác nhận thành công, Booking đã CONFIRMED")
    @APIResponse(responseCode = "400", description = "Payment không phải loại CASH hoặc đã xác nhận")
    public Response confirmCash(@PathParam("id") Long paymentId) {
        PaymentDto.PaymentResponse payment = paymentService.confirmCash(paymentId);
        return Response.ok(ApiResponse.success("Cash payment confirmed, booking is now CONFIRMED", payment)).build();
    }

    // ── VNPay Callback (redirect từ VNPay sau thanh toán) ────────────────────

    /**
     * GET /api/v1/payments/vnpay/callback
     * VNPay redirect khách hàng về URL này sau khi thanh toán.
     * Hệ thống xác minh chữ ký → cập nhật trạng thái → trả kết quả.
     *
     * Tham số chuẩn VNPay: vnp_TxnRef, vnp_ResponseCode, vnp_SecureHash, v.v.
     */
    @GET
    @Path("/vnpay/callback")
    @PermitAll
    @Operation(summary = "VNPay callback — xử lý kết quả thanh toán từ VNPay redirect")
    public Response vnpayCallbackGet(
            @QueryParam("vnp_TxnRef")        String txnRef,
            @QueryParam("vnp_ResponseCode")  String responseCode,
            @QueryParam("vnp_SecureHash")    String secureHash,
            @Context UriInfo uriInfo) {

        // Thu thập tất cả query params để verify chữ ký
        Map<String, String> allParams = new HashMap<>();
        uriInfo.getQueryParameters().forEach((k, v) -> {
            if (!v.isEmpty()) allParams.put(k, v.get(0));
        });

        String result = paymentService.processVnpayCallback(txnRef, responseCode, secureHash, allParams);
        return Response.ok(ApiResponse.success("VNPay callback processed: " + result, null)).build();
    }

    /**
     * POST /api/v1/payments/vnpay/callback
     * VNPay IPN (Instant Payment Notification) — server-to-server webhook.
     * Dùng khi VNPay gửi thông báo trực tiếp (không qua redirect).
     */
    @POST
    @Path("/vnpay/callback")
    @PermitAll
    @Operation(summary = "VNPay IPN webhook — xử lý thông báo server-to-server")
    public Response vnpayCallbackPost(
            @QueryParam("vnp_TxnRef")       String txnRef,
            @QueryParam("vnp_ResponseCode") String responseCode,
            @QueryParam("vnp_SecureHash")   String secureHash,
            @Context UriInfo uriInfo) {

        Map<String, String> allParams = new HashMap<>();
        uriInfo.getQueryParameters().forEach((k, v) -> {
            if (!v.isEmpty()) allParams.put(k, v.get(0));
        });

        paymentService.processVnpayCallback(txnRef, responseCode, secureHash, allParams);
        // VNPay yêu cầu response đúng format
        return Response.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}").build();
    }

}
