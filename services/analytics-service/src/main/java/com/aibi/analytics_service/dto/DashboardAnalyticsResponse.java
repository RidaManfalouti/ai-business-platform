package com.aibi.analytics_service.dto;

public record DashboardAnalyticsResponse(
        KpiSummaryResponse kpi,
        TrendsResponse trends,
        TopProductsResponse topProducts,
        StatusDistributionResponse statusDistribution,
        LowStockResponse lowStock
) {}
