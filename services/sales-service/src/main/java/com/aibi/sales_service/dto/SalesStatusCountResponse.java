package com.aibi.sales_service.dto;

import com.aibi.sales_service.model.SaleStatus;

public record SalesStatusCountResponse(
        SaleStatus status,
        long count
) {}
