package com.sportify.booking.client;

import com.sportify.common.dto.ApiResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.math.BigDecimal;

/**
 * REST Client để gọi sang field-service.
 *
 * Fault Tolerance:
 * - @Retry: tự động thử lại khi mạng lỗi tạm thời
 * - @Timeout: tránh treo thread vô thời hạn
 * - @CircuitBreaker: ngắt mạch khi field-service liên tục lỗi,
 *   tránh cascade failure sang toàn hệ thống
 *
 * URL cấu hình trong application.properties:
 *   field-service/mp-rest/url=http://localhost:8082
 */
@RegisterRestClient(configKey = "field-service")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FieldServiceClient {

    /**
     * Lấy thông tin sân theo ID.
     * Dùng để kiểm tra sân tồn tại, lấy tên sân + locationName làm snapshot.
     */
    @GET
    @Path("/fields/{id}")
    @Retry(maxRetries = 3, delay = 200)
    @Timeout(2000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
    ApiResponse<FieldDetail> getField(@PathParam("id") Long fieldId);

    /**
     * Kiểm tra sân có đang mở cửa (AVAILABLE) không.
     * Nếu MAINTENANCE → không cho đặt sân.
     */
    @GET
    @Path("/fields/{id}/availability")
    @Retry(maxRetries = 2, delay = 200)
    @Timeout(2000)
    ApiResponse<Boolean> checkAvailability(
            @PathParam("id") Long fieldId,
            @QueryParam("date") String date,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime
    );

    /**
     * Tính giá cho khung giờ đặt sân (Dynamic Pricing).
     * Kết quả được lưu vào Booking.totalPrice như snapshot.
     */
    @GET
    @Path("/fields/{id}/price")
    @Retry(maxRetries = 2, delay = 200)
    @Timeout(2000)
    ApiResponse<PriceDetail> calculatePrice(
            @PathParam("id") Long fieldId,
            @QueryParam("date") String date,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime
    );

    // ── Inner DTOs khớp với FieldResponse của field-service ───────────────────

    record FieldDetail(
            Long id,
            String name,
            String status,
            Long locationId,
            String locationName,
            String locationAddress,
            String locationRegion,
            Long fieldTypeId,
            String fieldTypeName,
            Integer playerCapacity,
            String sportName
    ) {}

    record PriceDetail(
            Long fieldId,
            String fieldName,
            java.math.BigDecimal pricePerHour,
            double durationHours,
            java.math.BigDecimal totalPrice,
            String currency,
            String dayType
    ) {}
}
