package com.sportify.payment.client;

import com.sportify.common.dto.ApiResponse;
import com.sportify.payment.dto.PaymentDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client để gọi sang booking-service.
 *
 * URL cấu hình trong application.properties:
 *   booking-service/mp-rest/url=http://localhost:8083
 *
 * Fault Tolerance:
 * - @Retry: thử lại khi mạng lỗi tạm thời
 * - @Timeout: tránh treo thread khi booking-service chậm
 */
@RegisterRestClient(configKey = "booking-service")
@Path("/api/v1/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BookingServiceClient {

    /**
     * Lấy thông tin booking để kiểm tra trước khi tạo thanh toán.
     * Dùng endpoint /internal để không cần kiểm tra userId.
     */
    @GET
    @Path("/{id}/internal")
    @Retry(maxRetries = 3, delay = 200)
    @Timeout(3000)
    ApiResponse<PaymentDto.BookingDetail> getBooking(@PathParam("id") Long bookingId);

    /**
     * Xác nhận booking sau khi thanh toán thành công.
     * Đây là bước quan trọng nhất trong luồng thanh toán:
     * Payment SUCCESS → gọi API này → Booking chuyển sang CONFIRMED.
     */
    @PATCH
    @Path("/{id}/confirm")
    @Retry(maxRetries = 3, delay = 500)
    @Timeout(5000)
    ApiResponse<Void> confirmBooking(@PathParam("id") Long bookingId);
}
