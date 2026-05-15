package com.sportify.booking.client;

import com.sportify.common.dto.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RegisterRestClient(configKey = "payment-service")
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
public interface PaymentServiceClient {

    @GET
    @Path("/booking/{bookingId}")
    ApiResponse<PaymentDetail> getByBookingId(@PathParam("bookingId") Long bookingId);

    @PATCH
    @Path("/booking/{bookingId}/confirm-cash")
    ApiResponse<PaymentDetail> confirmCashByBookingId(@PathParam("bookingId") Long bookingId);

    record PaymentDetail(
            Long id,
            Long bookingId,
            Long userId,
            BigDecimal amount,
            String paymentMethod,
            String paymentStatus,
            String txnRef,
            String paymentUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
