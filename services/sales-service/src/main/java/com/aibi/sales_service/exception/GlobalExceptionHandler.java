package com.aibi.sales_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SaleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSaleNotFound(SaleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCustomerNotFound(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidSaleStatusTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSaleStatusTransition(InvalidSaleStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "CONFLICT",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(DuplicateSaleReferenceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateSaleReference(DuplicateSaleReferenceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "CONFLICT",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "CONFLICT",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(RemoteServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleRemoteServiceUnavailable(RemoteServiceUnavailableException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.SERVICE_UNAVAILABLE;
        String errorCode = status == HttpStatus.BAD_GATEWAY ? "BAD_GATEWAY" : "SERVICE_UNAVAILABLE";
        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", errorCode,
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "BAD_REQUEST",
                        "message", "Format du corps de requête invalide ou données non lisibles."
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null
                                ? error.getDefaultMessage()
                                : "Valeur invalide.",
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "VALIDATION_ERROR",
                        "messages", errors
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "INTERNAL_SERVER_ERROR",
                        "message", "Une erreur inattendue est survenue : " + ex.getMessage()
                ));
    }
}
