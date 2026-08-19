package com.sportify.booking.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportify.booking.dto.BookingDto;
import com.sportify.booking.dto.NotificationDto;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE Resource — Tính năng 3 + 4:
 *   - Tính năng 3: Lịch sân cập nhật real-time (theo fieldId)
 *   - Tính năng 4: Thông báo real-time cho chủ sân (admin channel)
 *
 * Lưu ý: Dùng MultiEmitter<? super String> vì Mutiny dùng contravariant type
 * trong Multi.createFrom().emitter() — emitter thực tế là MultiEmitter<? super T>.
 */
@Path("/api/v1/bookings")
@ApplicationScoped
@Tag(name = "Booking SSE", description = "Real-time slot updates & owner notifications via SSE")
public class BookingSseResource {

    // fieldId → danh sách emitter (Tính năng 3)
    private final Map<Long, List<MultiEmitter<? super String>>> fieldEmitterMap = new ConcurrentHashMap<>();

    // Kênh admin — tất cả chủ sân đang subscribe (Tính năng 4)
    private final List<MultiEmitter<? super String>> adminEmitters = new CopyOnWriteArrayList<>();

    @Inject
    ObjectMapper objectMapper;

    // ── Tính năng 3: Lịch sân real-time ─────────────────────────────────────

    /**
     * GET /api/v1/bookings/events/field/{fieldId}
     *
     * Frontend trang xem lịch kết nối để nhận real-time slot updates.
     * Ví dụ: new EventSource('http://localhost:8083/api/v1/bookings/events/field/1')
     */
    @GET
    @Path("/events/field/{fieldId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Subscribe real-time slot updates theo fieldId (SSE)")
    public Multi<String> subscribeFieldSlots(@PathParam("fieldId") Long fieldId) {
        return Multi.createFrom().emitter(emitter -> {
            fieldEmitterMap.computeIfAbsent(fieldId, k -> new CopyOnWriteArrayList<>()).add(emitter);

            // Heartbeat khi kết nối
            emitter.emit(buildConnectedEvent("Field slot SSE connected for fieldId=" + fieldId));

            emitter.onTermination(() -> {
                List<MultiEmitter<? super String>> list = fieldEmitterMap.get(fieldId);
                if (list != null) {
                    list.remove(emitter);
                    if (list.isEmpty()) {
                        fieldEmitterMap.remove(fieldId);
                    }
                }
            });
        });
    }

    // ── Tính năng 4: Thông báo chủ sân real-time ────────────────────────────

    /**
     * GET /api/v1/bookings/events/admin
     *
     * Admin panel của chủ sân kết nối để nhận thông báo booking mới.
     * Ví dụ: new EventSource('http://localhost:8083/api/v1/bookings/events/admin')
     */
    @GET
    @Path("/events/admin")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Subscribe real-time new booking notifications cho chủ sân (SSE)")
    public Multi<String> subscribeAdminNotifications() {
        return Multi.createFrom().emitter(emitter -> {
            adminEmitters.add(emitter);

            // Heartbeat khi kết nối
            emitter.emit(buildConnectedEvent("Owner notification SSE connected"));

            emitter.onTermination(() -> adminEmitters.remove(emitter));
        });
    }

    // ── Push methods (được gọi bởi BookingService) ────────────────────────────

    /**
     * Push slot-update event khi booking mới được tạo hoặc bị huỷ.
     * Broadcast tới tất cả client đang xem lịch của fieldId này.
     */
    public void pushSlotUpdate(Long fieldId, LocalDate date, LocalTime startTime,
                               LocalTime endTime, String status) {
        List<MultiEmitter<? super String>> emitters = fieldEmitterMap.get(fieldId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String payload = buildSlotUpdatePayload(fieldId, date, startTime, endTime, status);
        broadcastToEmitters(emitters, payload);
    }

    /**
     * Push new-booking-notification event tới kênh admin (chủ sân).
     * Broadcast tới tất cả admin đang kết nối.
     */
    public void pushNewBookingNotification(NotificationDto notification) {
        if (adminEmitters.isEmpty()) {
            return;
        }
        String payload = buildNewBookingPayload(notification);
        broadcastToEmitters(adminEmitters, payload);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void broadcastToEmitters(List<MultiEmitter<? super String>> emitters, String payload) {
        List<MultiEmitter<? super String>> snapshot = List.copyOf(emitters);
        for (MultiEmitter<? super String> emitter : snapshot) {
            try {
                emitter.emit(payload);
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    private String buildSlotUpdatePayload(Long fieldId, LocalDate date, LocalTime startTime,
                                          LocalTime endTime, String status) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("event", "slot-update");
            data.put("fieldId", fieldId);
            data.put("date", date != null ? date.toString() : null);
            data.put("startTime", startTime != null ? startTime.toString() : null);
            data.put("endTime", endTime != null ? endTime.toString() : null);
            data.put("status", status);
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"event\":\"slot-update\",\"fieldId\":" + fieldId
                    + ",\"status\":\"" + status + "\"}";
        }
    }

    private String buildNewBookingPayload(NotificationDto notification) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("event", "new-booking-notification");
            data.put("notification", notification);
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"event\":\"new-booking-notification\",\"notificationId\":"
                    + (notification != null ? notification.id : "null") + "}";
        }
    }

    private String buildConnectedEvent(String message) {
        return "{\"event\":\"connected\",\"message\":\"" + message + "\"}";
    }
}
