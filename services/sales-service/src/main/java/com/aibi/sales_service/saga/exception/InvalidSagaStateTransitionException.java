package com.aibi.sales_service.saga.exception;

public class InvalidSagaStateTransitionException extends IllegalStateException {
    public InvalidSagaStateTransitionException(String message) {
        super(message);
    }
}
