package com.sportify.gateway.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "payment-service")
@Path("/api/v1/dashboard/payments")
public interface PaymentDashboardClient {

    @GET
    Response getDashboard(@HeaderParam("Authorization") String authorization,
                          @QueryParam("from") String from,
                          @QueryParam("to") String to);

    @GET
    @Path("/overview")
    Response getOverview(@HeaderParam("Authorization") String authorization,
                         @QueryParam("from") String from,
                         @QueryParam("to") String to);

    @GET
    @Path("/daily-revenue")
    Response getDailyRevenue(@HeaderParam("Authorization") String authorization,
                             @QueryParam("from") String from,
                             @QueryParam("to") String to);
}
