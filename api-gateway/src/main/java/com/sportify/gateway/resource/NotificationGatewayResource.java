package com.sportify.gateway.resource;

import com.sportify.gateway.client.NotificationServiceClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notification Gateway", description = "Proxy → booking-service notifications")
public class NotificationGatewayResource {

    @Inject
    @RestClient
    NotificationServiceClient notificationClient;

    @GET
    @PermitAll
    public Response getNotifications(@QueryParam("userId") Long userId,
                                     @QueryParam("search") String search,
                                     @QueryParam("page") @DefaultValue("0") int page,
                                     @QueryParam("size") @DefaultValue("10") int size) {
        return notificationClient.getNotifications(userId, search, page, size);
    }

    @GET
    @Path("/unread-count")
    @PermitAll
    public Response getUnreadCount(@QueryParam("userId") Long userId) {
        return notificationClient.getUnreadCount(userId);
    }

    @PUT
    @Path("/{id}/read")
    @PermitAll
    public Response markAsRead(@PathParam("id") Long id) {
        return notificationClient.markAsRead(id);
    }

    @PUT
    @Path("/read-all")
    @PermitAll
    public Response markAllAsRead(@QueryParam("userId") Long userId) {
        return notificationClient.markAllAsRead(userId);
    }
}
