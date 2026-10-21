package com.aibi.sales_service.exception;

public class DuplicateSaleReferenceException extends RuntimeException {
    public DuplicateSaleReferenceException(String message) {
        super(message);
    }
}
