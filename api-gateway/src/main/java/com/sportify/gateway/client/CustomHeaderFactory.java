package com.sportify.gateway.client;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

public class CustomHeaderFactory implements ClientHeadersFactory {
    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        
        // Forward Authorization
        if (incomingHeaders.containsKey("Authorization")) {
            result.put("Authorization", incomingHeaders.get("Authorization"));
        }
        
        // Forward X-User-Id
        if (incomingHeaders.containsKey("X-User-Id")) {
            result.put("X-User-Id", incomingHeaders.get("X-User-Id"));
        }
        
        return result;
    }
}
