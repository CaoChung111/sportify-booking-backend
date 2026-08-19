package com.sportify.gateway.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;

@RegisterRestClient(configKey = "booking-service")
@RegisterClientHeaders(CustomHeaderFactory.class)
@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BookingServiceClient {

    @GET
    @Path("/check-availability")
    Response checkAvailability(@QueryParam("fieldId") Long fieldId,
                               @QueryParam("date") String date,
                               @QueryParam("startTime") String startTime,
                               @QueryParam("endTime") String endTime);

    @POST
    Response create(@HeaderParam("Authorization") String authorization, Object body);

    @GET
    Response getMyBookings(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/admin/all")
    Response getAllBookings(@QueryParam("status") String status,
                            @QueryParam("search") String search,
                            @QueryParam("page") Integer page,
                            @QueryParam("size") Integer size,
                            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/field/{fieldId}")
    Response getByFieldAndDate(@PathParam("fieldId") Long fieldId,
                               @QueryParam("date") String date,
                               @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}")
    Response getById(@PathParam("id") Long id,
                     @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}/cancel")
    Response cancel(@PathParam("id") Long id,
                    @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}/confirm")
    Response confirm(@PathParam("id") Long id,
                     @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{id}/complete")
    Response complete(@PathParam("id") Long id,
                      @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}/internal")
    Response getByIdInternal(@PathParam("id") Long id,
                              @HeaderParam("Authorization") String authorization);
}
