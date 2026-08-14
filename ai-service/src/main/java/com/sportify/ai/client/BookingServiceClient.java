package com.sportify.ai.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client gọi sang booking-service để kiểm tra lịch đặt sân.
 * Dùng cho Function Calling khi AI cần tra cứu sân trống real-time.
 */
@RegisterRestClient(configKey = "booking-service")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BookingServiceClient {

    /**
     * Lấy danh sách booking cho một sân trong ngày cụ thể.
     * Dùng để xác định các slot đã bị chiếm.
     */
    @GET
    @Path("/bookings/field/{fieldId}")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
    Response getBookingsByField(@PathParam("fieldId") Long fieldId,
                                @QueryParam("date") String date);
}
