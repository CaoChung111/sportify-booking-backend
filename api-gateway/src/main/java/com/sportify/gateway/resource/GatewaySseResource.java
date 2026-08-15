package com.sportify.gateway.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE Resource — Tính năng 2: Dashboard Admin cập nhật Real-time
 *
 * Lưu ý: Dùng MultiEmitter<? super String> vì Mutiny dùng contravariant type
 * trong Multi.createFrom().emitter() — emitter thực tế là MultiEmitter<? super T>.
 */
@Path("/api/v1/events")
@ApplicationScoped
@Tag(name = "Gateway SSE", description = "Real-time dashboard updates via Server-Sent Events")
public class GatewaySseResource {

    // Danh sách tất cả admin client đang subscribe dashboard
    private final List<MultiEmitter<? super String>> dashboardEmitters = new CopyOnWriteArrayList<>();

    @Inject
    ObjectMapper objectMapper;

    /**
     * GET /api/v1/events/dashboard
     *
     * Admin dashboard kết nối để nhận real-time updates.
     * Ví dụ: new EventSource('http://localhost:8080/api/v1/events/dashboard')
     */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @PermitAll
    @Operation(summary = "Subscribe real-time admin dashboard updates (SSE)")
    public Multi<String> subscribeDashboard() {
        return Multi.createFrom().emitter(emitter -> {
            dashboardEmitters.add(emitter);

            // Heartbeat khi kết nối
            emitter.emit(buildConnectedEvent());

            emitter.onTermination(() -> dashboardEmitters.remove(emitter));
        });
    }

    /**
     * Broadcast dashboard-update event tới tất cả admin đang kết nối.
     * Gọi từ BookingGatewayResource khi có booking mới,
     * và từ PaymentGatewayResource khi có thanh toán thành công.
     *
     * @param type "new_booking" | "payment_success"
     * @param data Object dữ liệu kèm theo (có thể null)
     */
    public void broadcastDashboard(String type, Object data) {
        if (dashboardEmitters.isEmpty()) {
            return;
        }

        String payload = buildDashboardPayload(type, data);
        List<MultiEmitter<? super String>> snapshot = List.copyOf(dashboardEmitters);

        for (MultiEmitter<? super String> emitter : snapshot) {
            try {
                emitter.emit(payload);
            } catch (Exception e) {
                dashboardEmitters.remove(emitter);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildDashboardPayload(String type, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "dashboard-update");
            payload.put("type", type);
            if (data != null) {
                payload.put("data", data);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"event\":\"dashboard-update\",\"type\":\"" + type + "\"}";
        }
    }

    private String buildConnectedEvent() {
        return "{\"event\":\"connected\",\"message\":\"Dashboard SSE connected\"}";
    }
}
