package com.aibi.sales_service.saga.service;

import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.StockReleaseResponse;
import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.repository.SagaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
public class SagaRecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(SagaRecoveryWorker.class);

    private final SagaRepository sagaRepository;
    private final SagaService sagaService;
    private final ProductServiceClient productServiceClient;
    private final Random random = new Random();

    @Value("${saga.recovery.enabled:true}")
    private boolean recoveryEnabled;

    @Value("${saga.recovery.max-attempts:5}")
    private int maxAttempts;

    @Value("${saga.recovery.initial-backoff-ms:1000}")
    private long initialBackoffMs;

    @Value("${saga.recovery.max-backoff-ms:30000}")
    private long maxBackoffMs;

    @Value("${saga.recovery.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${saga.recovery.batch-size:10}")
    private int batchSize;

    public SagaRecoveryWorker(SagaRepository sagaRepository,
                              SagaService sagaService,
                              ProductServiceClient productServiceClient) {
        this.sagaRepository = sagaRepository;
        this.sagaService = sagaService;
        this.productServiceClient = productServiceClient;
    }

    @Scheduled(fixedDelayString = "${saga.recovery.interval-ms:5000}")
    public void scheduledRecovery() {
        if (!recoveryEnabled) {
            return;
        }
        recoverPendingSagas();
    }

    /**
     * Traite un lot de Sagas en attente de compensation dont l'échéance de réessai est atteinte.
     * Cette méthode peut être appelée directement dans les tests d'intégration.
     *
     * @return le nombre de Sagas traitées lors de ce cycle.
     */
    public int recoverPendingSagas() {
        LocalDateTime now = LocalDateTime.now();
        List<Saga> eligibleSagas;
        try {
            eligibleSagas = sagaRepository.findEligibleForRecovery(now, batchSize);
        } catch (Exception ex) {
            log.error("Erreur lors de la récupération des Sagas éligibles pour compensation : {}", ex.getMessage());
            return 0;
        }

        if (eligibleSagas.isEmpty()) {
            return 0;
        }

        log.info("Worker de reprise : {} Saga(s) éligible(s) pour tentative de compensation.", eligibleSagas.size());
        int processedCount = 0;

        for (Saga saga : eligibleSagas) {
            boolean claimed = sagaService.claimSagaForCompensation(saga.getSagaId());
            if (!claimed) {
                continue;
            }

            processSingleSagaCompensation(saga);
            processedCount++;
        }

        return processedCount;
    }

    private void processSingleSagaCompensation(Saga saga) {
        String reservationId = saga.getReservationId() != null && !saga.getReservationId().isBlank()
                ? saga.getReservationId()
                : saga.getSaleReference();

        log.info("Reprise compensation pour Saga {} (Réf: {}, Réservation: {}, Tentative: {}/{})",
                saga.getSagaId(), saga.getSaleReference(), reservationId, saga.getAttemptCount() + 1, maxAttempts);

        try {
            // Appel réseau idempotent exécuté EN DEHORS de toute transaction de base de données longue
            StockReleaseResponse response = productServiceClient.releaseStock(reservationId);
            log.info("Compensation réussie pour Saga {} : {}", saga.getSagaId(), response != null ? response.message() : "OK");
            sagaService.recordCompensationAttempt(saga.getSagaId(), true, null, null);
        } catch (Exception ex) {
            String errorMsg = ex.getMessage();
            int currentAttempt = saga.getAttemptCount() + 1;
            LocalDateTime nextRetry = calculateNextRetry(currentAttempt);

            log.warn("Tentative de compensation échouée pour Saga {} ({}). Prochain réessai à : {}",
                    saga.getSagaId(), errorMsg, nextRetry);

            sagaService.recordCompensationAttempt(saga.getSagaId(), false, errorMsg, nextRetry);
        }
    }

    public LocalDateTime calculateNextRetry(int attemptNumber) {
        // Formule exponentielle bornée : min(maxBackoff, initialBackoff * multiplier^(attempt-1))
        double exponentialDelay = initialBackoffMs * Math.pow(backoffMultiplier, Math.max(0, attemptNumber - 1));
        long boundedDelay = (long) Math.min(maxBackoffMs, exponentialDelay);

        // Ajout d'un jitter aléatoire (0 à 20% du délai) pour éviter les tempêtes de requêtes concurrentes (thundering herd)
        long jitter = boundedDelay > 0 ? (long) (random.nextDouble() * 0.2 * boundedDelay) : 0;
        long totalDelayMs = boundedDelay + jitter;

        return LocalDateTime.now().plusNanos(totalDelayMs * 1_000_000L);
    }
}
