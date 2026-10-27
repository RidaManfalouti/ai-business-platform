package com.aibi.analytics_service.exception;

import org.springframework.http.HttpStatus;

public class DownstreamServiceException extends RuntimeException {

    private final HttpStatus status;

    public DownstreamServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
