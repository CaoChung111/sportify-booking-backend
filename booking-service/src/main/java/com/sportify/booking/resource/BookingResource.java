package com.sportify.booking.resource;

import com.sportify.booking.dto.BookingDto;
import com.sportify.booking.service.BookingService;
import com.sportify.common.dto.ApiResponse;
import jakarta.annotation.security.PermitAll;
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
        // Dev mode: OIDC disabled, no JWT → dùng mock userId=1
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                return Long.valueOf(userIdClaim.toString());
            }
            // Fallback to sub if no userId claim
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return 1L; // map to userId=1 for dev
            }
        } catch (Exception e) {
            // Dev mode: JWT not available
        }
        return 1L; // dev fallback
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

    /**
     * Internal endpoint — called by payment-service to confirm booking after payment.
     * In a real system this would use mTLS or an internal network only.
     */
    @PATCH
    @Path("/{id}/confirm")
    @PermitAll
    @Operation(summary = "Confirm booking after payment (internal)")
    public Response confirm(@PathParam("id") Long id) {
        bookingService.confirm(id);
        return Response.ok(ApiResponse.success("Booking confirmed", null)).build();
    }

    /**
     * Internal: get booking by id without user check (for payment-service)
     */
    @GET
    @Path("/{id}/internal")
    @PermitAll
    @Operation(summary = "Get booking detail (internal use)")
    public Response getByIdInternal(@PathParam("id") Long id) {
        BookingDto.BookingResponse booking = bookingService.getByIdInternal(id);
        return Response.ok(ApiResponse.success(booking)).build();
    }
}
