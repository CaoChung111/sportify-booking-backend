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
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bookings", description = "Đặt sân, tra cứu và quản lý lịch đặt sân")
public class BookingResource {

    @Inject BookingService bookingService;
    @Inject JsonWebToken jwt;

    /**
     * Lấy userId từ JWT claim.
     */
    private Long currentUserId() {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null) {
                return Long.valueOf(userIdClaim.toString());
            }
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return 1L; // map keycloakId → userId=1 for dev
            }
        } catch (Exception e) {
            // Dev mode: JWT không khả dụng
        }
        return 1L;
    }

    // ── Check Availability ───────────────────────────────────────────────────

    /**
     * GET /api/v1/bookings/check-availability
     * Kiểm tra một khung giờ có trống không trước khi tạo booking.
     */
    @GET
    @Path("/check-availability")
    @Operation(summary = "Kiểm tra một khung giờ có trống hay không")
    @APIResponse(responseCode = "200", description = "Khung giờ trống")
    @APIResponse(responseCode = "409", description = "Khung giờ đã được đặt")
    public Response checkAvailability(
            @Parameter(description = "ID của sân", required = true) @QueryParam("fieldId") Long fieldId,
            @Parameter(description = "Ngày đặt (YYYY-MM-DD)", required = true) @QueryParam("date") LocalDate date,
            @Parameter(description = "Giờ bắt đầu (HH:mm)", required = true) @QueryParam("startTime") LocalTime startTime,
            @Parameter(description = "Giờ kết thúc (HH:mm)", required = true) @QueryParam("endTime") LocalTime endTime) {

        bookingService.checkSlotAvailability(fieldId, date, startTime, endTime);
        return Response.ok(ApiResponse.success("This time slot is available", null)).build();
    }

    // ── Tạo đặt sân ──────────────────────────────────────────────────────────

    /**
     * POST /api/v1/bookings
     * Tạo đơn đặt sân mới.
     */
    @POST
    @Operation(summary = "Tạo đơn đặt sân mới — kiểm tra xung đột lịch và tính giá tự động")
    @APIResponse(responseCode = "201", description = "Đặt sân thành công, trạng thái PENDING")
    @APIResponse(responseCode = "400", description = "Sân đang bảo trì hoặc thông tin không hợp lệ")
    @APIResponse(responseCode = "409", description = "Khung giờ đã được đặt bởi người khác")
    public Response create(@Valid BookingDto.CreateBookingRequest request) {
        BookingDto.BookingResponse booking = bookingService.create(currentUserId(), request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Booking created successfully", booking))
                .build();
    }

    // ── Lấy danh sách đặt sân ────────────────────────────────────────────────

    /**
     * GET /api/v1/bookings
     * Lấy tất cả đặt sân của user đang đăng nhập, sắp xếp mới nhất trước.
     */
    @GET
    @Operation(summary = "Lấy lịch sử đặt sân của user đang đăng nhập")
    @APIResponse(responseCode = "200", description = "Danh sách booking")
    public Response getMyBookings() {
        List<BookingDto.BookingResponse> bookings = bookingService.getMyBookings(currentUserId());
        return Response.ok(ApiResponse.success(bookings)).build();
    }

    /**
     * GET /api/v1/bookings/field/{fieldId}?date=yyyy-MM-dd
     * Lấy danh sách booking chưa huỷ của một sân trong một ngày.
     */
    @GET
    @Path("/field/{fieldId}")
    @Operation(summary = "Lấy danh sách booking của một sân theo ngày")
    @APIResponse(responseCode = "200", description = "Danh sách booking của sân trong ngày")
    @APIResponse(responseCode = "400", description = "Thiếu hoặc sai định dạng date")
    public Response getByFieldAndDate(@PathParam("fieldId") Long fieldId,
                                      @QueryParam("date") LocalDate date) {
        List<BookingDto.BookingResponse> bookings = bookingService.getBookingsByFieldAndDate(fieldId, date);
        return Response.ok(ApiResponse.success(bookings)).build();
    }

    // ── Chi tiết một booking ──────────────────────────────────────────────────

    /**
     * GET /api/v1/bookings/{id}
     * Lấy chi tiết booking — chỉ chủ đơn mới xem được.
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Lấy chi tiết đơn đặt sân theo ID")
    @APIResponse(responseCode = "200", description = "Chi tiết booking")
    @APIResponse(responseCode = "403", description = "Không có quyền xem booking này")
    @APIResponse(responseCode = "404", description = "Không tìm thấy booking")
    public Response getById(@PathParam("id") Long id) {
        BookingDto.BookingResponse booking = bookingService.getById(id, currentUserId());
        return Response.ok(ApiResponse.success(booking)).build();
    }

    // ── Huỷ đặt sân ──────────────────────────────────────────────────────────

    /**
     * PATCH /api/v1/bookings/{id}/cancel
     * Huỷ đặt sân — chỉ được huỷ khi trạng thái PENDING.
     */
    @PATCH
    @Path("/{id}/cancel")
    @Operation(summary = "Huỷ đặt sân (chỉ PENDING, chưa thanh toán)")
    @APIResponse(responseCode = "200", description = "Huỷ thành công")
    @APIResponse(responseCode = "400", description = "Không thể huỷ (đã xác nhận / đã hoàn thành)")
    public Response cancel(@PathParam("id") Long id) {
        bookingService.cancel(id, currentUserId());
        return Response.ok(ApiResponse.success("Booking cancelled successfully", null)).build();
    }

    // ── Internal: Confirm booking (Payment Service gọi) ───────────────────────

    /**
     * PATCH /api/v1/bookings/{id}/confirm
     * ⚠️ Endpoint nội bộ — chỉ Payment Service mới được phép gọi.
     */
    @PATCH
    @Path("/{id}/confirm")
    @PermitAll
    @Operation(summary = "[Internal] Xác nhận booking sau khi thanh toán (Payment Service only)")
    public Response confirm(@PathParam("id") Long id) {
        bookingService.confirm(id);
        return Response.ok(ApiResponse.success("Booking confirmed successfully", null)).build();
    }

    // ── Internal: Complete booking ────────────────────────────────────────────

    /**
     * PATCH /api/v1/bookings/{id}/complete
     * ⚠️ Endpoint nội bộ.
     */
    @PATCH
    @Path("/{id}/complete")
    @PermitAll
    @Operation(summary = "[Internal/Admin] Đánh dấu booking đã hoàn thành")
    public Response complete(@PathParam("id") Long id) {
        bookingService.complete(id);
        return Response.ok(ApiResponse.success("Booking marked as completed", null)).build();
    }

    // ── Internal: Get booking by ID (Payment Service gọi) ────────────────────

    /**
     * GET /api/v1/bookings/{id}/internal
     * ⚠️ Endpoint nội bộ.
     */
    @GET
    @Path("/{id}/internal")
    @PermitAll
    @Operation(summary = "[Internal] Lấy chi tiết booking không kiểm tra quyền (Payment Service only)")
    public Response getByIdInternal(@PathParam("id") Long id) {
        BookingDto.BookingResponse booking = bookingService.getByIdInternal(id);
        return Response.ok(ApiResponse.success(booking)).build();
    }
}
