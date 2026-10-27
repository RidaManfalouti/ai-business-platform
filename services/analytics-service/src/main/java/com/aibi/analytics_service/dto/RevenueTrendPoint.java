package com.aibi.analytics_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueTrendPoint(
        LocalDate date,
        long orderCount,
        BigDecimal revenue,
        String currency
) {}
