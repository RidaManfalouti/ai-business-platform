package com.aibi.sales_service.dto;

import java.math.BigDecimal;

public record TopCustomerAggregateResponse(
        Long customerId,
        long completedSalesCount,
        BigDecimal totalSpent
) {}
