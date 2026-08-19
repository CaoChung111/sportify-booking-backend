package com.sportify.gateway.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "booking-service")
@RegisterClientHeaders(CustomHeaderFactory.class)
@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface NotificationServiceClient {

    @GET
    Response getNotifications(@QueryParam("userId") Long userId,
                              @QueryParam("search") String search,
                              @QueryParam("page") int page,
                              @QueryParam("size") int size);

    @GET
    @Path("/unread-count")
    Response getUnreadCount(@QueryParam("userId") Long userId);

    @PUT
    @Path("/{id}/read")
    Response markAsRead(@PathParam("id") Long id);

    @PUT
    @Path("/read-all")
    Response markAllAsRead(@QueryParam("userId") Long userId);
}
