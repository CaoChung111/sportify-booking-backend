package com.sportify.common.exception;

import com.sportify.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

/**
 * Mapper này "bắt" các lỗi validation (ConstraintViolationException) được ném ra
 * khi một đối tượng DTO (ví dụ: CreateBookingRequest) không hợp lệ.
 *
 * Nó sẽ trích xuất tất cả các thông báo lỗi từ các annotation validation
 * (@NotNull, @FutureOrPresent, @Size, etc.) và trả về một lỗi 400 Bad Request
 * với một thông báo duy nhất, rõ ràng.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Lấy tất cả các thông báo lỗi từ các vi phạm validation
        String message = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        // Tạo một ApiResponse với thông báo lỗi đã được tổng hợp
        ApiResponse<Object> errorResponse = ApiResponse.error(message);

        // Trả về một response 400 Bad Request
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .build();
    }
}
