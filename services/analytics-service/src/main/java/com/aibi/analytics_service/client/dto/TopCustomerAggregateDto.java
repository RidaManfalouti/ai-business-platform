package com.aibi.analytics_service.client.dto;

import java.math.BigDecimal;

public record TopCustomerAggregateDto(
        Long customerId,
        long completedSalesCount,
        BigDecimal totalSpent
) {}
