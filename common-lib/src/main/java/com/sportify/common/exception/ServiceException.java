package com.sportify.common.exception;

public class ServiceException extends RuntimeException {

    private final int statusCode;

    public ServiceException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static ServiceException notFound(String resource, Long id) {
        return new ServiceException(404, resource + " not found with id: " + id);
    }

    public static ServiceException badRequest(String message) {
        return new ServiceException(400, message);
    }

    public static ServiceException conflict(String message) {
        return new ServiceException(409, message);
    }
}
