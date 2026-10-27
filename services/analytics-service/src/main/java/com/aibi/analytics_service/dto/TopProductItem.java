package com.aibi.analytics_service.dto;

import java.math.BigDecimal;

public record TopProductItem(
        Long productId,
        String productName,
        long quantitySold,
        BigDecimal revenue,
        String currency
) {}
