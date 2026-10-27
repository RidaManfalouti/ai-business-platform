package com.aibi.analytics_service.dto;

public record StatusDistributionItem(
        String status,
        long count,
        double percentage
) {}
