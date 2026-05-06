package com.sportify.booking.resource;

import com.sportify.booking.dto.BookingDto;
import com.sportify.booking.service.BookingService;
import com.sportify.common.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bookings")
public class BookingResource {

    @Inject BookingService bookingService;
    @Inject JsonWebToken jwt;

    private Long currentUserId() {
        // Extract userId from JWT claims
        return Long.valueOf(jwt.getClaim("userId").toString());
    }

    @POST
    @Operation(summary = "Create a new booking")
    public Response create(@Valid BookingDto.CreateBookingRequest request) {
        BookingDto.BookingResponse booking = bookingService.create(currentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Booking created", booking))
                .build();
    }

    @GET
    @Operation(summary = "Get my bookings")
    public Response getMyBookings() {
        List<BookingDto.BookingResponse> bookings = bookingService.getMyBookings(currentUserId());
        return Response.ok(ApiResponse.success(bookings)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get booking by ID")
    public Response getById(@PathParam("id") Long id) {
        BookingDto.BookingResponse booking = bookingService.getById(id, currentUserId());
        return Response.ok(ApiResponse.success(booking)).build();
    }

    @PATCH
    @Path("/{id}/cancel")
    @Operation(summary = "Cancel a booking")
    public Response cancel(@PathParam("id") Long id) {
        bookingService.cancel(id, currentUserId());
        return Response.ok(ApiResponse.success("Booking cancelled", null)).build();
    }
}
