package com.sportify.ai.service;

import com.sportify.ai.client.FieldServiceClient;
import com.sportify.ai.client.BookingServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@ApplicationScoped
public class FunctionExecutor {
    private static final Logger LOG = Logger.getLogger(FunctionExecutor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject @RestClient FieldServiceClient fieldClient;
    @Inject @RestClient BookingServiceClient bookingClient;

    /**
     * Thực thi function call từ Gemini.
     * Dispatch theo tên function và gọi REST client tương ứng.
     */
    public Object execute(String functionName, Map<String, Object> args) {
        LOG.infof("Executing function: %s with args: %s", functionName, args);
        try {
            return switch (functionName) {
                case "search_available_fields" -> searchFields(args);
                case "get_field_price" -> getFieldPrice(args);
                case "list_locations" -> listLocations();
                case "list_sports" -> listSports();
                case "get_field_types" -> getFieldTypes(args);
                case "get_price_table" -> getPriceTable(args);
                default -> Map.of("error", "Unknown function: " + functionName);
            };
        } catch (Exception e) {
            LOG.errorf(e, "Function execution failed: %s", functionName);
            return Map.of("error", "Function call failed: " + e.getMessage());
        }
    }
    
    private Long getLongArg(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key) || args.get(key) == null) return null;
        Object val = args.get(key);
        if (val instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getStringArg(Map<String, Object> args, String key) {
        if (args == null || !args.containsKey(key) || args.get(key) == null) return null;
        return args.get(key).toString();
    }

    private Object searchFields(Map<String, Object> args) throws Exception {
        Long locationId = getLongArg(args, "locationId");
        Long sportId = getLongArg(args, "sportId");
        String status = getStringArg(args, "status");
        if (status == null) status = "AVAILABLE";
        
        Response response = fieldClient.getFields(null, locationId, sportId, status, 0, 20);
        return parseResponse(response);
    }

    private Object getFieldPrice(Map<String, Object> args) throws Exception {
        Long fieldId = getLongArg(args, "fieldId");
        String date = getStringArg(args, "date");
        String startTime = getStringArg(args, "startTime");
        String endTime = getStringArg(args, "endTime");
        
        Response response = fieldClient.calculatePrice(fieldId, date, startTime, endTime);
        return parseResponse(response);
    }

    private Object listLocations() throws Exception {
        Response response = fieldClient.getLocations();
        return parseResponse(response);
    }

    private Object listSports() throws Exception {
        Response response = fieldClient.getSports();
        return parseResponse(response);
    }

    private Object getFieldTypes(Map<String, Object> args) throws Exception {
        Long sportId = getLongArg(args, "sportId");
        Response response = fieldClient.getFieldTypes(sportId);
        return parseResponse(response);
    }
    
    private Object getPriceTable(Map<String, Object> args) throws Exception {
        Long locationId = getLongArg(args, "locationId");
        Long fieldTypeId = getLongArg(args, "fieldTypeId");
        Response response = fieldClient.getPriceTable(locationId, fieldTypeId);
        return parseResponse(response);
    }
    
    private Object parseResponse(Response response) throws Exception {
        String json = response.readEntity(String.class);
        return objectMapper.readValue(json, Object.class);
    }
}
