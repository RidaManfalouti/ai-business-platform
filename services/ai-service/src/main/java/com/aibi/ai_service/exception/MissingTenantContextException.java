package com.aibi.ai_service.exception;

public class MissingTenantContextException extends RuntimeException {
    public MissingTenantContextException(String message) {
        super(message);
    }
}
