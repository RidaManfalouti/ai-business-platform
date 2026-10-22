package com.aibi.sales_service.client.dto;

public record StockReservationItemResponse(
        Long productId,
        Integer quantity
) {}
