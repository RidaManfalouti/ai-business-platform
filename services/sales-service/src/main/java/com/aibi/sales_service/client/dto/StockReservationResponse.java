package com.aibi.sales_service.client.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StockReservationResponse(
        String reservationId,
        String status,
        List<StockReservationItemResponse> items,
        LocalDateTime createdAt
) {}
