package com.sportify.gateway.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "payment-service")
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PaymentServiceClient {

    @POST
    Response initiate(@HeaderParam("Authorization") String authorization, Object body);

    @GET
    Response getMyPayments(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}")
    Response getById(@PathParam("id") Long id,
                     @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/booking/{bookingId}")
    Response getByBookingId(@PathParam("bookingId") Long bookingId,
                            @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}/confirm-cash")
    Response confirmCash(@PathParam("id") Long id,
                         @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/vnpay/callback")
    Response vnpayCallbackGet(@QueryParam("vnp_TxnRef") String txnRef,
                              @QueryParam("vnp_ResponseCode") String responseCode,
                              @QueryParam("vnp_SecureHash") String secureHash,
                              @QueryParam("vnp_Amount") String amount,
                              @QueryParam("vnp_BankCode") String bankCode,
                              @QueryParam("vnp_OrderInfo") String orderInfo,
                              @QueryParam("vnp_PayDate") String payDate,
                              @QueryParam("vnp_TransactionNo") String transactionNo);

    @POST
    @Path("/vnpay/callback")
    Response vnpayCallbackPost(@QueryParam("vnp_TxnRef") String txnRef,
                               @QueryParam("vnp_ResponseCode") String responseCode,
                               @QueryParam("vnp_SecureHash") String secureHash);

    @POST
    @Path("/momo/callback")
    Response momoCallback(Object body);
}
