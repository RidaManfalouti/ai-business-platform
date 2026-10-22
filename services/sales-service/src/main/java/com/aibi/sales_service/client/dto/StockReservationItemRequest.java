package com.aibi.sales_service.client.dto;

public record StockReservationItemRequest(
        Long productId,
        Integer quantity
) {}
