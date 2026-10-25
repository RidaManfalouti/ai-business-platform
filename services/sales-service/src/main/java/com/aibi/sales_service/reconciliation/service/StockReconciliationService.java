package com.aibi.sales_service.reconciliation.service;

import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.StockReservationResponse;
import com.aibi.sales_service.entity.Sale;
import com.aibi.sales_service.model.SaleStatus;
import com.aibi.sales_service.reconciliation.dto.ReconciliationDiscrepancy;
import com.aibi.sales_service.reconciliation.dto.ReconciliationReport;
import com.aibi.sales_service.reconciliation.model.DiscrepancyType;
import com.aibi.sales_service.repository.SaleRepository;
import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.repository.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StockReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(StockReconciliationService.class);

    private final SaleRepository saleRepository;
    private final SagaRepository sagaRepository;
    private final ProductServiceClient productServiceClient;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    private final AtomicReference<ReconciliationReport> latestReport = new AtomicReference<>();

    @Value("${saga.reconciliation.enabled:true}")
    private boolean reconciliationEnabled;

    @Value("${saga.reconciliation.stale-threshold-minutes:15}")
    private int staleThresholdMinutes;

    @org.springframework.beans.factory.annotation.Autowired
    public StockReconciliationService(SaleRepository saleRepository,
                                      SagaRepository sagaRepository,
                                      ProductServiceClient productServiceClient,
                                      org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider) {
        this.saleRepository = saleRepository;
        this.sagaRepository = sagaRepository;
        this.productServiceClient = productServiceClient;
        this.meterRegistry = meterRegistryProvider != null ? meterRegistryProvider.getIfAvailable() : null;
    }

    public StockReconciliationService(SaleRepository saleRepository,
                                      SagaRepository sagaRepository,
                                      ProductServiceClient productServiceClient) {
        this.saleRepository = saleRepository;
        this.sagaRepository = sagaRepository;
        this.productServiceClient = productServiceClient;
        this.meterRegistry = null;
    }

    @Scheduled(fixedDelayString = "${saga.reconciliation.interval-ms:60000}")
    public void scheduledReconciliation() {
        if (!reconciliationEnabled) {
            return;
        }
        runReconciliation(Duration.ofMinutes(staleThresholdMinutes));
    }

    /**
     * Exécute un audit de réconciliation en lecture seule entre les ventes locales, les Sagas
     * et l'état effectif des réservations dans Product Service.
     *
     * Invariant strict : Cette méthode ne modifie JAMAIS directement la quantité de stock en base.
     * Elle consigne des alertes structurées et produit un rapport opérationnel destiné aux administrateurs.
     */
    @Transactional(readOnly = true)
    public ReconciliationReport runReconciliation(Duration staleThreshold) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thresholdTime = now.minus(staleThreshold);
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        // 1. Détection des Sagas nécessitant une intervention manuelle (FAILED_REQUIRES_ATTENTION)
        List<Saga> attentionSagas = sagaRepository.findByStatus(SagaStatus.FAILED_REQUIRES_ATTENTION);
        for (Saga saga : attentionSagas) {
            discrepancies.add(new ReconciliationDiscrepancy(
                    DiscrepancyType.SAGA_REQUIRES_MANUAL_ATTENTION,
                    saga.getSaleReference(),
                    saga.getReservationId(),
                    null,
                    saga.getStatus().name(),
                    null,
                    "La Saga a épuisé toutes ses tentatives de compensation (" + saga.getAttemptCount() + "/" + saga.getMaxAttempts() + "). Erreur: " + saga.getLastError(),
                    "Déclencher un réessai via POST /api/sales/sagas/" + saga.getSagaId() + "/retry ou résoudre manuellement via /api/sales/sagas/" + saga.getSagaId() + "/resolve"
            ));
            log.warn("[STOCK_RECONCILIATION_DISCREPANCY] Type: SAGA_REQUIRES_MANUAL_ATTENTION, SagaId: {}, Ref: {}, Erreur: {}",
                    saga.getSagaId(), saga.getSaleReference(), saga.getLastError());
        }

        // 2. Détection des Sagas en cours de compensation anormalement anciennes (stale compensation)
        List<Saga> staleCompensations = sagaRepository.findStaleSagas(
                List.of(SagaStatus.COMPENSATION_PENDING, SagaStatus.COMPENSATING),
                thresholdTime
        );
        for (Saga saga : staleCompensations) {
            discrepancies.add(new ReconciliationDiscrepancy(
                    DiscrepancyType.STALE_UNFINISHED_COMPENSATION,
                    saga.getSaleReference(),
                    saga.getReservationId(),
                    null,
                    saga.getStatus().name(),
                    null,
                    "Compensation en attente depuis plus de " + staleThreshold.toMinutes() + " minutes sans aboutir.",
                    "Vérifier la connectivité avec Product Service et forcer un réessai via /api/sales/sagas/" + saga.getSagaId() + "/retry"
            ));
            log.warn("[STOCK_RECONCILIATION_DISCREPANCY] Type: STALE_UNFINISHED_COMPENSATION, SagaId: {}, Ref: {}, Créé le: {}",
                    saga.getSagaId(), saga.getSaleReference(), saga.getCreatedAt());
        }

        // 3. Détection des ventes CANCELLED dont la réservation est toujours active (RESERVED) dans Product Service
        List<Sale> cancelledSales = saleRepository.findByStatus(SaleStatus.CANCELLED, PageRequest.of(0, 50)).getContent();
        for (Sale sale : cancelledSales) {
            if (sale.getUpdatedAt() != null && sale.getUpdatedAt().isBefore(thresholdTime)) {
                try {
                    StockReservationResponse remoteRes = productServiceClient.getReservation(sale.getReference());
                    if (remoteRes != null && "RESERVED".equalsIgnoreCase(remoteRes.status())) {
                        discrepancies.add(new ReconciliationDiscrepancy(
                                DiscrepancyType.CANCELLED_SALE_RESERVATION_STILL_RESERVED,
                                sale.getReference(),
                                sale.getReference(),
                                sale.getStatus().name(),
                                null,
                                remoteRes.status(),
                                "La vente est annulée mais la réservation " + sale.getReference() + " est toujours marquée RESERVED dans Product Service.",
                                "Libérer manuellement la réservation via POST /api/products/reservations/" + sale.getReference() + "/release"
                        ));
                        log.warn("[STOCK_RECONCILIATION_DISCREPANCY] Type: CANCELLED_SALE_RESERVATION_STILL_RESERVED, Ref: {}", sale.getReference());
                    }
                } catch (Exception ex) {
                    log.debug("Impossible de vérifier l'état de la réservation pour la vente {} : {}", sale.getReference(), ex.getMessage());
                }
            }
        }

        ReconciliationReport report = new ReconciliationReport(
                now,
                attentionSagas.size() + staleCompensations.size(),
                cancelledSales.size(),
                discrepancies.size(),
                discrepancies
        );

        latestReport.set(report);
        if (meterRegistry != null) {
            meterRegistry.counter("stock_reconciliation_runs_total",
                    "result", discrepancies.isEmpty() ? "clean" : "discrepancies"
            ).increment();
        }
        if (discrepancies.isEmpty()) {
            log.info("Rapport de réconciliation de stock : 0 anomalie détectée (audit conforme).");
        } else {
            log.warn("Rapport de réconciliation de stock : {} anomalie(s) détectée(s) nécessitant une attention opérationnelle.", discrepancies.size());
        }

        return report;
    }

    public ReconciliationReport getLatestReport() {
        return latestReport.get();
    }
}
