package com.sportify.gateway.client;

import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestStreamElementType;

@RegisterRestClient(configKey = "booking-service")
@RegisterClientHeaders(CustomHeaderFactory.class)
@Path("/api/v1/bookings")
public interface BookingSseClient {

    @GET
    @Path("/events/admin")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    Multi<String> streamDashboardEvents(@HeaderParam("Authorization") String authorization);
}
