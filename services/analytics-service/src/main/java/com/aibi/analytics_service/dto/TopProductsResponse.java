package com.aibi.analytics_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record TopProductsResponse(
        List<TopProductItem> items,
        long totalQuantitySold,
        BigDecimal totalRevenue,
        String currency
) {}
