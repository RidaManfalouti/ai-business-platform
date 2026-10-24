package com.aibi.sales_service.reconciliation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReconciliationReport(
        LocalDateTime executedAt,
        int sagasChecked,
        int salesChecked,
        int discrepanciesCount,
        List<ReconciliationDiscrepancy> discrepancies
) {}
