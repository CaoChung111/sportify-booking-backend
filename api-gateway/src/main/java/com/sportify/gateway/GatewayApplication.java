package com.sportify.gateway;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
public class GatewayApplication extends Application {
    // API Gateway - routes requests to downstream services
    // OIDC JWT validation happens here via quarkus-oidc
}
