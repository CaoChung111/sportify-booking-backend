package com.sportify.gateway.resource;

import com.sportify.gateway.client.BookingSseClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import io.smallrye.mutiny.subscription.Cancellable;

import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/api/v1/ws/admin")
@ApplicationScoped
public class GatewayWebsocketResource {

    private static final Logger LOG = Logger.getLogger(GatewayWebsocketResource.class);

    @Inject
    @RestClient
    BookingSseClient bookingSseClient;

    private final ConcurrentHashMap<String, Cancellable> subscriptions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        LOG.info("WebSocket connection opened: " + session.getId());
        
        // Extract token from query parameters: ?token=...
        String token = null;
        String queryString = session.getQueryString();
        if (queryString != null && queryString.contains("token=")) {
            String[] params = queryString.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
        }
        
        String bearer = (token != null && !token.isEmpty()) ? "Bearer " + token : "";

        try {
            Cancellable sub = bookingSseClient.streamDashboardEvents(bearer)
                    .subscribe()
                    .with(
                            event -> {
                                LOG.debug("Sending WS message: " + event);
                                session.getAsyncRemote().sendText(event);
                            },
                            failure -> {
                                LOG.error("SSE stream failed", failure);
                                try { session.close(); } catch (Exception ignored) {}
                            },
                            () -> {
                                LOG.info("SSE stream completed");
                                try { session.close(); } catch (Exception ignored) {}
                            }
                    );

            subscriptions.put(session.getId(), sub);
        } catch (Exception e) {
            LOG.error("Failed to subscribe to SSE", e);
            try { session.close(); } catch (Exception ignored) {}
        }
    }

    @OnClose
    public void onClose(Session session) {
        LOG.info("WebSocket connection closed: " + session.getId());
        Cancellable sub = subscriptions.remove(session.getId());
        if (sub != null) {
            sub.cancel();
        }
    }
}
