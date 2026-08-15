package com.sportify.payment.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE Resource — Tính năng 1: Trạng thái thanh toán Real-time
 *
 * Frontend subscribe theo bookingId, nhận event khi payment callback hoàn tất.
 * Event format: { "bookingId": 1, "status": "SUCCESS|FAILED", "txnRef": "SPF1..." }
 *
 * Lưu ý: Dùng MultiEmitter<? super String> vì Mutiny dùng contravariant type
 * trong Multi.createFrom().emitter() — emitter thực tế là MultiEmitter<? super T>.
 */
@Path("/api/v1/payments")
@ApplicationScoped
@Tag(name = "Payment SSE", description = "Real-time payment status via Server-Sent Events")
public class PaymentSseResource {

    // bookingId → danh sách emitter (hỗ trợ nhiều tab/client cho cùng booking)
    // Dùng wildcard "? super String" để tương thích với kiểu trả về của Mutiny emitter
    private final Map<Long, List<MultiEmitter<? super String>>> emitterMap = new ConcurrentHashMap<>();

    @Inject
    ObjectMapper objectMapper;

    /**
     * GET /api/v1/payments/events/{bookingId}
     *
     * Frontend kết nối để nhận real-time payment status.
     * Ví dụ: new EventSource('http://localhost:8084/api/v1/payments/events/42')
     */
    @GET
    @Path("/events/{bookingId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Subscribe real-time payment status theo bookingId (SSE)")
    public Multi<String> subscribePaymentStatus(@PathParam("bookingId") Long bookingId) {
        return Multi.createFrom().emitter(emitter -> {
            // Đăng ký emitter vào map
            emitterMap.computeIfAbsent(bookingId, k -> new CopyOnWriteArrayList<>()).add(emitter);

            // Gửi heartbeat ngay khi kết nối để giữ connection sống
            emitter.emit(buildHeartbeat());

            // Dọn dẹp khi client disconnect
            emitter.onTermination(() -> {
                List<MultiEmitter<? super String>> list = emitterMap.get(bookingId);
                if (list != null) {
                    list.remove(emitter);
                    if (list.isEmpty()) {
                        emitterMap.remove(bookingId);
                    }
                }
            });
        });
    }

    /**
     * Push payment-status event tới tất cả client đang subscribe bookingId này.
     * Được gọi bởi PaymentService sau khi xử lý VNPay callback hoặc xác nhận CASH.
     *
     * @param bookingId ID của booking
     * @param status    "SUCCESS" | "FAILED" | "PAID_PENDING_CONFIRMATION" | "CANCELLED"
     * @param txnRef    Mã giao dịch nội bộ
     */
    public void push(Long bookingId, String status, String txnRef) {
        List<MultiEmitter<? super String>> emitters = emitterMap.get(bookingId);
        if (emitters == null || emitters.isEmpty()) {
            return; // Không có client nào đang subscribe
        }

        String payload = buildPaymentStatusPayload(bookingId, status, txnRef);

        // Broadcast tới tất cả emitter đang active cho bookingId này
        List<MultiEmitter<? super String>> snapshot = List.copyOf(emitters);
        for (MultiEmitter<? super String> emitter : snapshot) {
            try {
                emitter.emit(payload);
            } catch (Exception e) {
                // Emitter đã đóng hoặc lỗi — xóa khỏi list
                emitters.remove(emitter);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildPaymentStatusPayload(Long bookingId, String status, String txnRef) {
        try {
            Map<String, Object> data = Map.of(
                    "event", "payment-status",
                    "bookingId", bookingId,
                    "status", status,
                    "txnRef", txnRef != null ? txnRef : ""
            );
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"event\":\"payment-status\",\"bookingId\":" + bookingId
                    + ",\"status\":\"" + status + "\",\"txnRef\":\"\"}";
        }
    }

    private String buildHeartbeat() {
        return "{\"event\":\"connected\",\"message\":\"Payment SSE connected\"}";
    }
}
