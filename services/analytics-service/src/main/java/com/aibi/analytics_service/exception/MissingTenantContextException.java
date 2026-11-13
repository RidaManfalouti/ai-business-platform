package com.aibi.analytics_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class MissingTenantContextException extends RuntimeException {

    public MissingTenantContextException(String message) {
        super(message);
    }
}
