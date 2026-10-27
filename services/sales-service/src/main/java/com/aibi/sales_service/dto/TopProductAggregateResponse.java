package com.aibi.sales_service.dto;

import java.math.BigDecimal;

public record TopProductAggregateResponse(
        Long productId,
        String productName,
        long totalQuantity,
        BigDecimal totalRevenue
) {}
