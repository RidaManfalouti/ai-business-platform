package com.aibi.analytics_service.client.dto;

import java.math.BigDecimal;

public record SalesSummaryDto(
        BigDecimal totalRevenue,
        long completedSalesCount,
        long totalSalesCount
) {}
