package com.aibi.sales_service.reconciliation.dto;

import com.aibi.sales_service.reconciliation.model.DiscrepancyType;

public record ReconciliationDiscrepancy(
        DiscrepancyType type,
        String saleReference,
        String reservationId,
        String saleStatus,
        String sagaStatus,
        String reservationStatus,
        String description,
        String suggestedAction
) {}
