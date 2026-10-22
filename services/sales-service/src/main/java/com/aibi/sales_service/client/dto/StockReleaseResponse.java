package com.aibi.sales_service.client.dto;

public record StockReleaseResponse(
        String reservationId,
        String status,
        String message
) {}
