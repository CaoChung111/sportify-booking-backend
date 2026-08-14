package com.sportify.ai.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.core.Response;

/**
 * REST Client gọi sang field-service để lấy thông tin sân, giá, môn thể thao.
 * Dùng cho Function Calling của AI Chatbot.
 */
@RegisterRestClient(configKey = "field-service")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FieldServiceClient {

    @GET
    @Path("/fields")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
    Response getFields(@QueryParam("name") String name,
                       @QueryParam("locationId") Long locationId,
                       @QueryParam("sportId") Long sportId,
                       @QueryParam("status") String status,
                       @QueryParam("page") Integer page,
                       @QueryParam("size") Integer size);

    @GET
    @Path("/fields/{id}")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    Response getFieldById(@PathParam("id") Long id);

    @GET
    @Path("/fields/{id}/availability")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(2000)
    Response checkAvailability(@PathParam("id") Long fieldId);

    @GET
    @Path("/fields/{id}/price")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    Response calculatePrice(@PathParam("id") Long fieldId,
                            @QueryParam("date") String date,
                            @QueryParam("startTime") String startTime,
                            @QueryParam("endTime") String endTime);

    @GET
    @Path("/locations")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    Response getLocations();

    @GET
    @Path("/sports")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    Response getSports();

    @GET
    @Path("/field-types")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    Response getFieldTypes(@QueryParam("sportId") Long sportId);

    @GET
    @Path("/prices/table")
    @Retry(maxRetries = 2, delay = 300)
    @Timeout(3000)
    Response getPriceTable(@QueryParam("locationId") Long locationId,
                           @QueryParam("fieldTypeId") Long fieldTypeId);
}
