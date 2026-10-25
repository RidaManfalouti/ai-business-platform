package com.aibi.sales_service.saga.observability;

import com.aibi.sales_service.reconciliation.dto.ReconciliationReport;
import com.aibi.sales_service.reconciliation.model.DiscrepancyType;
import com.aibi.sales_service.reconciliation.service.StockReconciliationService;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.repository.SagaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Composant d'observabilité dédié pour les métriques de fiabilité des Sagas,
 * de réconciliation de stock et de communication interservices.
 *
 * Utilise uniquement des métriques agrégées et des labels à cardinalité bornée
 * (aucun identifiant d'entité, de vente, de saga ou d'utilisateur).
 */
@Component
public class SagaMetrics implements MeterBinder {

    private final SagaRepository sagaRepository;
    private final StockReconciliationService reconciliationService;
    private MeterRegistry meterRegistry;

    public SagaMetrics(SagaRepository sagaRepository, StockReconciliationService reconciliationService) {
        this.sagaRepository = sagaRepository;
        this.reconciliationService = reconciliationService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        this.meterRegistry = registry;

        // 1. Nombre de Sagas par statut (STARTED, STOCK_RESERVED, COMPLETED, COMPENSATION_PENDING, COMPENSATING, FAILED_REQUIRES_ATTENTION, etc.)
        for (SagaStatus status : SagaStatus.values()) {
            Gauge.builder("saga_status_count", () -> sagaRepository.countByStatus(status))
                    .description("Nombre actuel de Sagas selon leur statut")
                    .tag("status", status.name())
                    .register(registry);
        }

        // 2. Ancienneté maximale des opérations bloquées ou en attente de compensation (en secondes)
        Gauge.builder("saga_stuck_operations_max_age_seconds", this::getOldestStuckCompensationAgeSeconds)
                .description("Âge en secondes de la plus ancienne Saga en attente ou en cours de compensation")
                .tag("type", "compensation")
                .register(registry);

        Gauge.builder("saga_stuck_operations_max_age_seconds", this::getOldestPendingOperationAgeSeconds)
                .description("Âge en secondes de la plus ancienne Saga non complétée en phase d'exécution")
                .tag("type", "started")
                .register(registry);

        // 3. Anomalies de réconciliation par type de divergence
        for (DiscrepancyType type : DiscrepancyType.values()) {
            Gauge.builder("stock_reconciliation_anomalies_count", () -> getDiscrepanciesCountForType(type))
                    .description("Nombre d'anomalies détectées lors du dernier audit de réconciliation par type")
                    .tag("type", type.name())
                    .register(registry);
        }

        Gauge.builder("stock_reconciliation_discrepancies_total", this::getTotalDiscrepancies)
                .description("Nombre total d'anomalies relevées lors du dernier audit de réconciliation du stock")
                .register(registry);
    }

    private double getOldestStuckCompensationAgeSeconds() {
        return sagaRepository.findOldestStuckCompensationCreatedAt()
                .map(created -> (double) Math.max(0, Duration.between(created, LocalDateTime.now()).getSeconds()))
                .orElse(0.0);
    }

    private double getOldestPendingOperationAgeSeconds() {
        return sagaRepository.findOldestPendingOperationCreatedAt()
                .map(created -> (double) Math.max(0, Duration.between(created, LocalDateTime.now()).getSeconds()))
                .orElse(0.0);
    }

    private double getDiscrepanciesCountForType(DiscrepancyType type) {
        ReconciliationReport report = reconciliationService.getLatestReport();
        if (report == null || report.discrepancies() == null) {
            return 0.0;
        }
        return report.discrepancies().stream()
                .filter(d -> d.type() == type)
                .count();
    }

    private double getTotalDiscrepancies() {
        ReconciliationReport report = reconciliationService.getLatestReport();
        return (report != null) ? report.discrepanciesCount() : 0.0;
    }

    /**
     * Enregistre le résultat d'une tentative de reprise de compensation par le worker d'arrière-plan.
     */
    public void recordRecoveryAttempt(boolean success) {
        if (meterRegistry != null) {
            meterRegistry.counter("saga_compensation_recovery_attempts_total",
                    "outcome", success ? "success" : "failure"
            ).increment();
        }
    }

    /**
     * Enregistre l'exécution d'un cycle d'audit de réconciliation de stock.
     */
    public void recordReconciliationRun(int discrepancyCount) {
        if (meterRegistry != null) {
            meterRegistry.counter("stock_reconciliation_runs_total",
                    "result", discrepancyCount == 0 ? "clean" : "discrepancies"
            ).increment();
        }
    }

    /**
     * Enregistre un échec ou une indisponibilité réseau lors d'un appel à Product Service.
     */
    public void recordProductServiceClientError(String operation, String errorType) {
        if (meterRegistry != null) {
            meterRegistry.counter("product_service_client_errors_total",
                    "operation", operation != null ? operation : "unknown",
                    "error_type", errorType != null ? errorType : "unknown"
            ).increment();
        }
    }
}
