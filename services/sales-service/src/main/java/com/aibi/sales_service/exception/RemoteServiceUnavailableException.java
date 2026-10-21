package com.aibi.sales_service.exception;

import org.springframework.http.HttpStatus;

public class RemoteServiceUnavailableException extends RuntimeException {

    private final HttpStatus status;

    public RemoteServiceUnavailableException(String message) {
        this(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public RemoteServiceUnavailableException(String message, HttpStatus status) {
        super(message);
        this.status = status != null ? status : HttpStatus.SERVICE_UNAVAILABLE;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
