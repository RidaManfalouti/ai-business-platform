package com.aibi.analytics_service.dto;

import java.util.List;

public record StatusDistributionResponse(
        List<StatusDistributionItem> distribution,
        long totalOrders
) {}
