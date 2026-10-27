package com.aibi.analytics_service.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTrendDto(
        LocalDate date,
        long orderCount,
        BigDecimal totalRevenue
) {}
