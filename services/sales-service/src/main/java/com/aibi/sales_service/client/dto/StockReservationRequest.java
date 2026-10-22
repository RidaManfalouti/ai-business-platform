package com.aibi.sales_service.client.dto;

import java.util.List;

public record StockReservationRequest(
        String reservationId,
        List<StockReservationItemRequest> items
) {}
