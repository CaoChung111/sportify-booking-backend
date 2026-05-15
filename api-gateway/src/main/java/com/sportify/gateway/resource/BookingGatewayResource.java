package com.sportify.gateway.resource;

import com.sportify.gateway.client.BookingServiceClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Booking Gateway", description = "Proxy → booking-service (port 8083)")
public class BookingGatewayResource {

    @Inject
    @RestClient
    BookingServiceClient bookingClient;

    @POST
    @Operation(summary = "Tạo đơn đặt sân mới")
    public Response create(@Context HttpHeaders headers, Object body) {
        return bookingClient.create(headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @GET
    @Operation(summary = "Lấy lịch sử đặt sân của user đang đăng nhập")
    public Response getMyBookings(@Context HttpHeaders headers) {
        return bookingClient.getMyBookings(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Lấy chi tiết đơn đặt sân theo ID")
    public Response getById(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return bookingClient.getById(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @PATCH
    @Path("/{id}/cancel")
    @Operation(summary = "Huỷ đặt sân (chỉ PENDING, chưa thanh toán)")
    public Response cancel(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return bookingClient.cancel(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @PATCH
    @Path("/{id}/confirm")
    @PermitAll
    @Operation(summary = "[Internal] Xác nhận booking sau khi thanh toán")
    public Response confirm(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return bookingClient.confirm(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @PATCH
    @Path("/{id}/complete")
    @PermitAll
    @Operation(summary = "[Internal/Admin] Đánh dấu booking đã hoàn thành")
    public Response complete(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return bookingClient.complete(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @GET
    @Path("/{id}/internal")
    @PermitAll
    @Operation(summary = "[Internal] Lấy chi tiết booking không kiểm tra quyền")
    public Response getByIdInternal(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return bookingClient.getByIdInternal(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }
}
