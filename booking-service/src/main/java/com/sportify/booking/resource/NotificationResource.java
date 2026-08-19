package com.sportify.booking.resource;

import com.sportify.booking.service.NotificationService;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/notifications")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    @Inject
    NotificationService notificationService;

    @GET
    @PermitAll
    public Response getNotifications(
            @QueryParam("userId") Long userId,
            @QueryParam("search") String search,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        return Response.ok(notificationService.getNotifications(userId, search, page, size)).build();
    }

    @GET
    @Path("/unread-count")
    @PermitAll
    public Response getUnreadCount(@QueryParam("userId") Long userId) {
        return Response.ok(notificationService.getUnreadCount(userId)).build();
    }

    @PUT
    @Path("/{id}/read")
    @PermitAll
    public Response markAsRead(@PathParam("id") Long id) {
        notificationService.markAsRead(id);
        return Response.noContent().build();
    }

    @PUT
    @Path("/read-all")
    @PermitAll
    public Response markAllAsRead(@QueryParam("userId") Long userId) {
        notificationService.markAllAsRead(userId);
        return Response.noContent().build();
    }
}
