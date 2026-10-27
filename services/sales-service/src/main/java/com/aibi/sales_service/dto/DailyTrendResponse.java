package com.aibi.sales_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTrendResponse(
        LocalDate date,
        long orderCount,
        BigDecimal totalRevenue
) {}
