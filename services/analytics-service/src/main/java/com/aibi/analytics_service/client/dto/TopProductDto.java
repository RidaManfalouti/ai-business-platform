package com.aibi.analytics_service.client.dto;

import java.math.BigDecimal;

public record TopProductDto(
        Long productId,
        String productName,
        long totalQuantity,
        BigDecimal totalRevenue
) {}
