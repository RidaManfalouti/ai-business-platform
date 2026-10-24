package com.aibi.sales_service.saga.observability;

import com.aibi.sales_service.reconciliation.dto.ReconciliationReport;
import com.aibi.sales_service.reconciliation.service.StockReconciliationService;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.repository.SagaRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SagaHealthIndicator implements HealthIndicator {

    private final SagaRepository sagaRepository;
    private final StockReconciliationService reconciliationService;

    public SagaHealthIndicator(SagaRepository sagaRepository, StockReconciliationService reconciliationService) {
        this.sagaRepository = sagaRepository;
        this.reconciliationService = reconciliationService;
    }

    @Override
    public Health health() {
        long pendingCompensations = sagaRepository.countByStatus(SagaStatus.COMPENSATION_PENDING)
                + sagaRepository.countByStatus(SagaStatus.COMPENSATING);
        long requiresAttention = sagaRepository.countByStatus(SagaStatus.FAILED_REQUIRES_ATTENTION);

        ReconciliationReport latestReport = reconciliationService.getLatestReport();
        int discrepancies = latestReport != null ? latestReport.discrepanciesCount() : 0;

        Health.Builder builder = Health.up()
                .withDetail("pendingCompensations", pendingCompensations)
                .withDetail("requiresAttention", requiresAttention)
                .withDetail("reconciliationDiscrepancies", discrepancies);

        if (requiresAttention > 0) {
            builder.withDetail("warning", "Des Sagas ont échoué et requièrent une intervention manuelle (/api/sales/sagas?status=FAILED_REQUIRES_ATTENTION)");
        }

        return builder.build();
    }
}
