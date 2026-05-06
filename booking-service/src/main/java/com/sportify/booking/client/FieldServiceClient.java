package com.sportify.booking.client;

import com.sportify.common.dto.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

/**
 * REST Client để gọi sang field-service.
 * URL được cấu hình trong application.properties:
 *   field-service/mp-rest/url=http://localhost:8082
 */
@RegisterRestClient(configKey = "field-service")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FieldServiceClient {

    @GET
    @Path("/fields/{id}")
    @Retry(maxRetries = 3)
    @Timeout(2000)
    @CircuitBreaker(requestVolumeThreshold = 10)
    ApiResponse<FieldDetail> getField(@PathParam("id") Long fieldId);

    @GET
    @Path("/fields/{id}/availability")
    @Retry(maxRetries = 2)
    @Timeout(2000)
    ApiResponse<Boolean> checkAvailability(
            @PathParam("id") Long fieldId,
            @QueryParam("date") String date,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime
    );

    @GET
    @Path("/fields/{id}/price")
    @Retry(maxRetries = 2)
    @Timeout(2000)
    ApiResponse<PriceDetail> calculatePrice(
            @PathParam("id") Long fieldId,
            @QueryParam("date") String date,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime
    );

    // ── Inner DTOs matching field-service responses ─────────────────────
    record FieldDetail(Long id, String name, String locationName, String status) {}
    record PriceDetail(java.math.BigDecimal totalPrice) {}
}
