package com.aibi.sales_service.dto;

import java.math.BigDecimal;

public record SalesSummaryAggregateResponse(
        BigDecimal totalRevenue,
        long completedSalesCount,
        long totalSalesCount
) {}
