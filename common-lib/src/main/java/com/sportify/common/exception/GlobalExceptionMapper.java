package com.sportify.common.exception;

import com.sportify.common.dto.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<ServiceException> {

    @Override
    public Response toResponse(ServiceException ex) {
        return Response.status(ex.getStatusCode())
                .entity(ApiResponse.error(ex.getMessage()))
                .build();
    }
}
