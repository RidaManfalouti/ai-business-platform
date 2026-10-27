package com.aibi.analytics_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record TrendsResponse(
        List<RevenueTrendPoint> trends,
        BigDecimal totalPeriodRevenue,
        long totalPeriodOrders,
        String currency
) {}
