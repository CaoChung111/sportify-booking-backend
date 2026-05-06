package com.sportify.payment.client;

import com.sportify.common.dto.ApiResponse;
import com.sportify.payment.dto.PaymentDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client để gọi sang booking-service.
 * URL cấu hình trong application.properties:
 *   booking-service/mp-rest/url=http://localhost:8083
 */
@RegisterRestClient(configKey = "booking-service")
@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BookingServiceClient {

    @GET
    @Path("/{id}/internal")
    ApiResponse<PaymentDto.BookingDetail> getBooking(@PathParam("id") Long bookingId);

    @PATCH
    @Path("/{id}/confirm")
    ApiResponse<Void> confirmBooking(@PathParam("id") Long bookingId);
}
