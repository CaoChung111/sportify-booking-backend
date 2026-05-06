package com.sportify.payment.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.payment.dto.PaymentDto;
import com.sportify.payment.service.PaymentService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments")
public class PaymentResource {

    @Inject PaymentService paymentService;
    @Inject JsonWebToken jwt;

    private Long currentUserId() {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                return Long.valueOf(userIdClaim.toString());
            }
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return 1L;
            }
        } catch (Exception e) {
            // Dev mode: JWT not available
        }
        return 1L; // dev fallback
    }

    @POST
    @Operation(summary = "Initiate payment for a booking")
    public Response initiate(@Valid PaymentDto.CreatePaymentRequest request) {
        PaymentDto.PaymentResponse payment = paymentService.initiate(currentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Payment initiated", payment))
                .build();
    }

    @GET
    @Operation(summary = "Get my payment history")
    public Response getMyPayments() {
        List<PaymentDto.PaymentResponse> payments = paymentService.getByUserId(currentUserId());
        return Response.ok(ApiResponse.success(payments)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get payment by ID")
    public Response getById(@PathParam("id") Long id) {
        PaymentDto.PaymentResponse payment = paymentService.getById(id);
        return Response.ok(ApiResponse.success(payment)).build();
    }

    @GET
    @Path("/booking/{bookingId}")
    @Operation(summary = "Get payment by booking ID")
    public Response getByBookingId(@PathParam("bookingId") Long bookingId) {
        PaymentDto.PaymentResponse payment = paymentService.getByBookingId(bookingId);
        return Response.ok(ApiResponse.success(payment)).build();
    }

    /**
     * VNPay/MoMo gọi callback sau khi xử lý thanh toán.
     * Endpoint này KHÔNG yêu cầu auth (gateway gọi từ bên ngoài).
     */
    @GET
    @Path("/vnpay/callback")
    @PermitAll
    @Operation(summary = "VNPay payment callback (GET - called by VNPay redirect)")
    public Response vnpayCallbackGet(@QueryParam("vnp_TxnRef") String txnRef,
                                     @QueryParam("vnp_ResponseCode") String responseCode,
                                     @QueryParam("vnp_SecureHash") String secureHash) {
        paymentService.processVnpayCallback(txnRef, responseCode);
        return Response.ok(ApiResponse.success("Payment processed", null)).build();
    }

    @POST
    @Path("/vnpay/callback")
    @PermitAll
    @Operation(summary = "VNPay payment callback (POST)")
    public Response vnpayCallback(@QueryParam("vnp_TxnRef") String txnRef,
                                   @QueryParam("vnp_ResponseCode") String responseCode) {
        paymentService.processVnpayCallback(txnRef, responseCode);
        return Response.ok().build();
    }

    @POST
    @Path("/momo/callback")
    @PermitAll
    @Operation(summary = "MoMo payment callback")
    public Response momoCallback(PaymentDto.MomoCallbackRequest request) {
        paymentService.processMomoCallback(request);
        return Response.ok().build();
    }
}
