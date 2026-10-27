package com.aibi.analytics_service.dto;

import java.math.BigDecimal;

public record LowStockItem(
        Long productId,
        String name,
        String category,
        BigDecimal price,
        Integer stockQuantity,
        String stockStatus
) {}
