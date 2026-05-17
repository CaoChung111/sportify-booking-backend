package com.sportify.gateway.resource;

import com.sportify.gateway.client.BookingDashboardClient;
import com.sportify.gateway.client.PaymentDashboardClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/v1/dashboard")
@Tag(name = "Dashboard Gateway", description = "Thong ke dashboard admin")
public class DashboardGatewayResource {

    @Inject
    @RestClient
    BookingDashboardClient bookingDashboardClient;

    @Inject
    @RestClient
    PaymentDashboardClient paymentDashboardClient;

    @GET
    @Path("/bookings")
    @Operation(summary = "Tong hop dashboard booking")
    public Response getBookingDashboard(@Context HttpHeaders headers,
                                        @QueryParam("from") String from,
                                        @QueryParam("to") String to,
                                        @QueryParam("topLimit") Integer topLimit) {
        return bookingDashboardClient.getDashboard(headers.getHeaderString(HttpHeaders.AUTHORIZATION), from, to, topLimit);
    }

    @GET
    @Path("/bookings/overview")
    @Operation(summary = "Thong ke tong quan booking")
    public Response getBookingOverview(@Context HttpHeaders headers,
                                       @QueryParam("from") String from,
                                       @QueryParam("to") String to) {
        return bookingDashboardClient.getOverview(headers.getHeaderString(HttpHeaders.AUTHORIZATION), from, to);
    }

    @GET
    @Path("/bookings/daily")
    @Operation(summary = "Thong ke booking theo ngay")
    public Response getBookingDaily(@Context HttpHeaders headers,
                                    @QueryParam("from") String from,
                                    @QueryParam("to") String to) {
        return bookingDashboardClient.getDaily(headers.getHeaderString(HttpHeaders.AUTHORIZATION), from, to);
    }

    @GET
    @Path("/bookings/top-fields")
    @Operation(summary = "Top san theo booking")
    public Response getTopFields(@Context HttpHeaders headers,
                                 @QueryParam("from") String from,
                                 @QueryParam("to") String to,
                                 @QueryParam("limit") Integer limit) {
        return bookingDashboardClient.getTopFields(headers.getHeaderString(HttpHeaders.AUTHORIZATION), from, to, limit);
    }

    @GET
    @Path("/payments")
    @Operation(summary = "Tong hop dashboard payment")
    public Response getPaymentDashboard(@Context HttpHeaders headers,
                                        @QueryParam("from") String from,
                                        @QueryParam("to") String to) {
        return paymentDashboardClient.getDashboard(headers.getHeaderString(HttpHeaders.AUTHORIZATION), from, to);
    }

    @GET
    @Path("/payments/overview")
    @Operation(summary = "Thong ke tong quan payment")
    public Response getPaymentOverview(@Context HttpHeaders headers,
                                       @QueryParam("from") String from,
                                       @QueryParam("to") String to) {
        return paymentDashboardClient.getOverview(headers.getHeaderString(HttpHeaders.AUTHORIZATION), from, to);
    }

    @GET
    @Path("/payments/daily-revenue")
    @Operation(summary = "Doanh thu payment thanh cong theo ngay")
    public Response getDailyRevenue(@Context HttpHeaders headers,
                                    @QueryParam("from") String from,
                                    @QueryParam("to") String to) {
        return paymentDashboardClient.getDailyRevenue(headers.getHeaderString(HttpHeaders.AUTHORIZATION), from, to);
    }
}
