package com.sportify.gateway.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "booking-service")
@Path("/api/v1/dashboard/bookings")
public interface BookingDashboardClient {

    @GET
    Response getDashboard(@HeaderParam("Authorization") String authorization,
                          @QueryParam("from") String from,
                          @QueryParam("to") String to,
                          @QueryParam("topLimit") Integer topLimit);

    @GET
    @Path("/overview")
    Response getOverview(@HeaderParam("Authorization") String authorization,
                         @QueryParam("from") String from,
                         @QueryParam("to") String to);

    @GET
    @Path("/daily")
    Response getDaily(@HeaderParam("Authorization") String authorization,
                      @QueryParam("from") String from,
                      @QueryParam("to") String to);

    @GET
    @Path("/top-fields")
    Response getTopFields(@HeaderParam("Authorization") String authorization,
                          @QueryParam("from") String from,
                          @QueryParam("to") String to,
                          @QueryParam("limit") Integer limit);
}
