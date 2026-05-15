package com.sportify.gateway.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper này "bắt" các exception được ném ra bởi REST Client khi một service con
 * (ví dụ: booking-service, field-service) trả về một phản hồi lỗi (như 4xx hoặc 5xx).
 *
 * Thay vì để gateway mặc định trả về lỗi 500 chung chung, mapper này sẽ "bóc tách"
 * phản hồi gốc từ service con và chuyển tiếp mã trạng thái (status code) và
 * nội dung lỗi (error body) của nó đến client ban đầu.
 */
@Provider
public class GatewayExceptionMapper implements ExceptionMapper<ClientWebApplicationException> {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionMapper.class);

    @Override
    public Response toResponse(ClientWebApplicationException exception) {
        Response originalResponse = exception.getResponse();
        if (originalResponse == null) {
            log.error("ClientWebApplicationException không có phản hồi gốc", exception);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"success\":false, \"message\":\"Lỗi không xác định tại API Gateway.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        int statusCode = originalResponse.getStatus();
        log.warn("Service con đã trả về lỗi. Đang chuyển tiếp phản hồi. Status: {}", statusCode, exception);

        // Đọc nội dung lỗi gốc dưới dạng một chuỗi JSON thô.
        // Đây là cách an toàn nhất để đảm bảo không có thông tin nào bị mất.
        String errorJson = "{\"success\":false, \"message\":\"Không thể đọc được nội dung lỗi từ service con.\"}";
        if (originalResponse.hasEntity()) {
            try {
                // Phải buffer để có thể đọc lại nếu cần
                originalResponse.bufferEntity();
                errorJson = originalResponse.readEntity(String.class);
            } catch (Exception e) {
                log.error("Không thể đọc nội dung lỗi của service con.", e);
            }
        }

        // Xây dựng một response mới với status code và nội dung JSON gốc.
        // Trả về đúng kiểu `application/json` để trình duyệt hiểu.
        return Response.status(statusCode)
                .entity(errorJson)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
