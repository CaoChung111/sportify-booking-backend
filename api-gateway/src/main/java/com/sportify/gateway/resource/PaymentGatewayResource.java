package com.sportify.gateway.resource;

import com.sportify.gateway.client.PaymentServiceClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payment Gateway", description = "Proxy → payment-service (port 8084)")
public class PaymentGatewayResource {

    @Inject
    @RestClient
    PaymentServiceClient paymentClient;

    @Inject
    GatewaySseResource gatewaySseResource;

    @POST
    @Operation(summary = "Khởi tạo thanh toán cho booking (VNPAY | MoMo | CASH)")
    public Response initiate(@Context HttpHeaders headers, Object body) {
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        String token  = authHeader.substring(7);
        try {
            Response response = paymentClient.initiate(token, body);
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                try { gatewaySseResource.broadcastDashboard("payment_initiated", null); } catch (Exception ignored) {}
            }
            return response;

        } catch (org.jboss.resteasy.reactive.ClientWebApplicationException e) {
            String errorBody = e.getResponse().readEntity(String.class);
            System.out.println("==== LỖI TỪ PAYMENT SERVICE ====");
            System.out.println("Mã lỗi: " + e.getResponse().getStatus());
            System.out.println("Chi tiết: " + errorBody);
            System.out.println("================================");
            throw e;
        }
    }

    @GET
    @Operation(summary = "Lấy lịch sử thanh toán của user đang đăng nhập")
    public Response getMyPayments(@Context HttpHeaders headers) {
        return paymentClient.getMyPayments(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Lấy chi tiết thanh toán theo ID")
    public Response getById(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return paymentClient.getById(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @GET
    @Path("/booking/{bookingId}")
    @Operation(summary = "Lấy thông tin thanh toán theo Booking ID")
    public Response getByBookingId(@PathParam("bookingId") Long bookingId, @Context HttpHeaders headers) {
        return paymentClient.getByBookingId(bookingId, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @PATCH
    @Path("/{id}/confirm-cash")
    @Operation(summary = "Admin xác nhận thanh toán tiền mặt (CASH)")
    public Response confirmCash(@PathParam("id") Long id, @Context HttpHeaders headers) {
        Response response = paymentClient.confirmCash(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            try { gatewaySseResource.broadcastDashboard("payment_success", null); } catch (Exception ignored) {}
        }
        return response;
    }

    @GET
    @Path("/vnpay/callback")
    @PermitAll
    @Operation(summary = "VNPay callback — xử lý kết quả thanh toán từ VNPay redirect")
    public Response vnpayCallbackGet(
            @QueryParam("vnp_TxnRef")        String txnRef,
            @QueryParam("vnp_ResponseCode")  String responseCode,
            @QueryParam("vnp_SecureHash")    String secureHash,
            @QueryParam("vnp_Amount")        String amount,
            @QueryParam("vnp_BankCode")      String bankCode,
            @QueryParam("vnp_OrderInfo")     String orderInfo,
            @QueryParam("vnp_PayDate")       String payDate,
            @QueryParam("vnp_TransactionNo") String transactionNo) {
        Response response = paymentClient.vnpayCallbackGet(
                txnRef, responseCode, secureHash, amount, bankCode, orderInfo, payDate, transactionNo);
        // SSE: Broadcast dashboard-update khi thanh toán thành công (responseCode="00")
        if ("00".equals(responseCode)) {
            try {
                gatewaySseResource.broadcastDashboard("payment_success", null);
            } catch (Exception ignored) {
                // Không ảnh hưởng luồng redirect chính
            }
        }
        return response;
    }

    @POST
    @Path("/vnpay/callback")
    @PermitAll
    @Operation(summary = "VNPay IPN webhook — xử lý thông báo server-to-server")
    public Response vnpayCallbackPost(
            @QueryParam("vnp_TxnRef")       String txnRef,
            @QueryParam("vnp_ResponseCode") String responseCode,
            @QueryParam("vnp_SecureHash")   String secureHash) {
        Response response = paymentClient.vnpayCallbackPost(txnRef, responseCode, secureHash);
        // SSE: Broadcast dashboard-update khi IPN success
        if ("00".equals(responseCode)) {
            try {
                gatewaySseResource.broadcastDashboard("payment_success", null);
            } catch (Exception ignored) {
                // Không ảnh hưởng luồng IPN
            }
        }
        return response;
    }

    @POST
    @Path("/momo/callback")
    @PermitAll
    @Operation(summary = "MoMo IPN webhook — xử lý thông báo thanh toán từ MoMo")
    public Response momoCallback(Object body) {
        return paymentClient.momoCallback(body);
    }
}
