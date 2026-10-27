package com.aibi.analytics_service.dto;

import java.util.List;

public record LowStockResponse(
        List<LowStockItem> items,
        long lowStockCount,
        int threshold
) {}
