package com.aibi.analytics_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record KpiSummaryResponse(
        BigDecimal totalRevenue,
        long completedSalesCount,
        BigDecimal averageOrderValue,
        long totalSalesCount,
        long totalCustomers,
        long totalProducts,
        String currency,
        Instant periodStart,
        Instant periodEnd,
        String timezone
) {}
