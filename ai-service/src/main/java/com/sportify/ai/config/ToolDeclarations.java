package com.sportify.ai.config;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Khai báo các công cụ (Function Calling) cho Gemini AI.
 */
@ApplicationScoped
public class ToolDeclarations {

    public List<Map<String, Object>> getToolDeclarations() {
        List<Map<String, Object>> functionDeclarations = new ArrayList<>();

        // 1. search_available_fields
        functionDeclarations.add(createFunction(
                "search_available_fields",
                "Tìm danh sách sân thể thao, có thể lọc theo môn thể thao và địa điểm",
                Map.of(
                        "sportId", createSchema("INTEGER", "ID môn thể thao"),
                        "locationId", createSchema("INTEGER", "ID địa điểm"),
                        "status", createSchema("STRING", "Trạng thái sân, mặc định là AVAILABLE")
                ),
                List.of()
        ));

        // 2. get_field_price
        functionDeclarations.add(createFunction(
                "get_field_price",
                "Tính giá thuê sân cho một khung giờ cụ thể (Dynamic Pricing)",
                Map.of(
                        "fieldId", createSchema("INTEGER", "ID sân"),
                        "date", createSchema("STRING", "Ngày đặt sân (YYYY-MM-DD)"),
                        "startTime", createSchema("STRING", "Giờ bắt đầu (HH:mm)"),
                        "endTime", createSchema("STRING", "Giờ kết thúc (HH:mm)")
                ),
                List.of("fieldId", "date", "startTime", "endTime")
        ));

        // 3. list_locations
        functionDeclarations.add(createFunction(
                "list_locations",
                "Lấy danh sách tất cả các cơ sở thể thao trong hệ thống",
                Map.of(),
                List.of()
        ));

        // 4. list_sports
        functionDeclarations.add(createFunction(
                "list_sports",
                "Lấy danh sách tất cả các môn thể thao được hỗ trợ",
                Map.of(),
                List.of()
        ));

        // 5. get_field_types
        functionDeclarations.add(createFunction(
                "get_field_types",
                "Lấy danh sách loại sân (VD: sân 5 người, sân 7 người), có thể lọc theo môn thể thao",
                Map.of(
                        "sportId", createSchema("INTEGER", "ID môn thể thao")
                ),
                List.of()
        ));

        // 6. get_price_table
        functionDeclarations.add(createFunction(
                "get_price_table",
                "Lấy bảng giá chi tiết theo địa điểm và loại sân",
                Map.of(
                        "locationId", createSchema("INTEGER", "ID địa điểm"),
                        "fieldTypeId", createSchema("INTEGER", "ID loại sân")
                ),
                List.of()
        ));

        Map<String, Object> tool = new HashMap<>();
        tool.put("functionDeclarations", functionDeclarations);
        return List.of(tool);
    }

    private Map<String, Object> createFunction(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "OBJECT");
        if (!properties.isEmpty()) {
            parameters.put("properties", properties);
        }
        if (!required.isEmpty()) {
            parameters.put("required", required);
        }
        function.put("parameters", parameters);
        
        return function;
    }

    private Map<String, Object> createSchema(String type, String description) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", type);
        schema.put("description", description);
        return schema;
    }
}
