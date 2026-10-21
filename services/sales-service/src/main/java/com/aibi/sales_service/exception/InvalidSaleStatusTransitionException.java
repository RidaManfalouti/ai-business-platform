package com.aibi.sales_service.exception;

public class InvalidSaleStatusTransitionException extends RuntimeException {
    public InvalidSaleStatusTransitionException(String message) {
        super(message);
    }
}
