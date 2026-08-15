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

    @Inject
    GatewaySseResource gatewaySseResource;

    @GET
    @Path("/check-availability")
    @PermitAll
    @Operation(summary = "Kiểm tra một khung giờ có trống hay không")
    public Response checkAvailability(
            @QueryParam("fieldId") Long fieldId,
            @QueryParam("date") String date,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime) {
        return bookingClient.checkAvailability(fieldId, date, startTime, endTime);
    }

    @POST
    @Operation(summary = "Tạo đơn đặt sân mới")
    public Response create(@Context HttpHeaders headers, Object body) {
        Response response = bookingClient.create(headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
        // SSE: Broadcast dashboard-update khi có booking mới thành công
        if (response.getStatus() == 201) {
            try {
                gatewaySseResource.broadcastDashboard("new_booking", null);
            } catch (Exception ignored) {
                // Không làm gán đến luồng proxy chính nếu SSE broadcast thất bại
            }
        }
        return response;
    }

    @GET
    @Operation(summary = "Lấy lịch sử đặt sân của user đang đăng nhập")
    public Response getMyBookings(@Context HttpHeaders headers) {
        return bookingClient.getMyBookings(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @GET
    @Path("/admin/all")
    @Operation(summary = "Lấy toàn bộ lịch sử đặt sân (Admin only)")
    public Response getAllBookings(@Context HttpHeaders headers) {
        return bookingClient.getAllBookings(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @GET
    @Path("/field/{fieldId}")
    @Operation(summary = "Lấy danh sách booking của một sân theo ngày")
    public Response getByFieldAndDate(@PathParam("fieldId") Long fieldId,
                                      @QueryParam("date") String date,
                                      @Context HttpHeaders headers) {
        return bookingClient.getByFieldAndDate(
                fieldId,
                date,
                headers.getHeaderString(HttpHeaders.AUTHORIZATION));
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
        Response response = bookingClient.cancel(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            try { gatewaySseResource.broadcastDashboard("booking_cancelled", null); } catch (Exception ignored) {}
        }
        return response;
    }

    @PATCH
    @Path("/{id}/confirm")
    @PermitAll
    @Operation(summary = "[Internal] Xác nhận booking sau khi thanh toán")
    public Response confirm(@PathParam("id") Long id, @Context HttpHeaders headers) {
        Response response = bookingClient.confirm(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            try { gatewaySseResource.broadcastDashboard("booking_confirmed", null); } catch (Exception ignored) {}
        }
        return response;
    }

    @PATCH
    @Path("/{id}/complete")
    @PermitAll
    @Operation(summary = "[Internal/Admin] Đánh dấu booking đã hoàn thành")
    public Response complete(@PathParam("id") Long id, @Context HttpHeaders headers) {
        Response response = bookingClient.complete(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            try { gatewaySseResource.broadcastDashboard("booking_completed", null); } catch (Exception ignored) {}
        }
        return response;
    }

    @GET
    @Path("/{id}/internal")
    @PermitAll
    @Operation(summary = "[Internal] Lấy chi tiết booking không kiểm tra quyền")
    public Response getByIdInternal(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return bookingClient.getByIdInternal(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }
}
