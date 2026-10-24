package com.aibi.sales_service.saga;

import com.aibi.sales_service.client.CustomerServiceClient;
import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.CustomerDto;
import com.aibi.sales_service.client.dto.ProductDto;
import com.aibi.sales_service.client.dto.StockReleaseResponse;
import com.aibi.sales_service.client.dto.StockReservationResponse;
import com.aibi.sales_service.dto.CreateSaleRequest;
import com.aibi.sales_service.dto.SaleItemRequest;
import com.aibi.sales_service.dto.UpdateSaleStatusRequest;
import com.aibi.sales_service.exception.RemoteServiceUnavailableException;
import com.aibi.sales_service.model.SaleStatus;
import com.aibi.sales_service.repository.SaleRepository;
import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.entity.SagaEvent;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;
import com.aibi.sales_service.saga.repository.SagaEventRepository;
import com.aibi.sales_service.saga.repository.SagaRepository;
import com.aibi.sales_service.saga.service.SagaRecoveryWorker;
import com.aibi.sales_service.saga.service.SagaService;
import com.aibi.sales_service.service.SalesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@WithMockUser(username = "admin", roles = {"ADMIN"})
class SagaFailureAndRecoveryIntegrationTest {

    @Autowired
    private SalesService salesService;

    @Autowired
    private SagaService sagaService;

    @Autowired
    private SagaRepository sagaRepository;

    @Autowired
    private SagaEventRepository sagaEventRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SagaRecoveryWorker sagaRecoveryWorker;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ProductServiceClient productServiceClient;

    @MockitoBean
    private CustomerServiceClient customerServiceClient;

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void setUp() {
        saleRepository.deleteAll();
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
    }

    @Test
    @DisplayName("Injection de panne : Panne product-service lors du rollback immédiat -> Saga persiste en COMPENSATION_PENDING pour reprise différée")
    void shouldPersistSagaInCompensationPendingWhenImmediateRollbackFails() {
        Long customerId = 100L;
        Long productId = 200L;
        String ref = "CMD-FAIL-IMM-" + randomSuffix();

        when(customerServiceClient.getCustomerById(customerId))
                .thenReturn(new CustomerDto(customerId, "Test", "User", "test@example.com", "0102030405", "Corp"));
        when(productServiceClient.getProductById(productId))
                .thenReturn(new ProductDto(productId, "Serveur Rack", "Serveur 2U", new BigDecimal("1200.00"), 10, "Matériel"));

        StockReservationResponse resResponse = new StockReservationResponse(
                ref,
                "RESERVED",
                Collections.emptyList(),
                LocalDateTime.now()
        );
        when(productServiceClient.reserveStock(any())).thenReturn(resResponse);

        // Création initiale valide de la vente
        CreateSaleRequest request = new CreateSaleRequest(
                customerId,
                ref,
                List.of(new SaleItemRequest(productId, 1))
        );
        var createdSale = salesService.createSale(request);
        assertThat(createdSale).isNotNull();

        // Simulation panne product-service lors du rollback/compensation immédiat de l'annulation
        org.mockito.Mockito.doThrow(new RemoteServiceUnavailableException("Product Service 503 lors de la compensation", HttpStatus.SERVICE_UNAVAILABLE))
                .when(productServiceClient).releaseStock(ref);

        // Déclenchement de l'annulation : la compensation synchrone échoue
        salesService.updateSaleStatus(createdSale.id(), new UpdateSaleStatusRequest(SaleStatus.CANCELLED));

        // Vérifier l'état dans PostgreSQL : une Saga d'annulation doit persister en COMPENSATION_PENDING
        List<Saga> pendingSagas = sagaRepository.findByStatus(SagaStatus.COMPENSATION_PENDING);
        assertThat(pendingSagas).hasSize(1);
        Saga failedSaga = pendingSagas.get(0);

        assertThat(failedSaga.getSaleReference()).isEqualTo(ref);
        assertThat(failedSaga.getReservationId()).isEqualTo(ref);
        assertThat(failedSaga.getStatus()).isEqualTo(SagaStatus.COMPENSATION_PENDING);
        assertThat(failedSaga.getAttemptCount()).isGreaterThanOrEqualTo(1);
        assertThat(failedSaga.getNextRetryAt()).isNotNull();
        assertThat(failedSaga.getLastError()).contains("Product Service 503");
    }

    @Test
    @DisplayName("Reprise automatique : Échec transitoire au réessai 1, puis succès au réessai 2 -> Saga COMPENSATED")
    void shouldRecoverSagaAfterTransientFailureOnRetry() {
        String sagaId = "SAGA-RETRY-" + randomSuffix();
        String ref = "REF-RETRY-" + randomSuffix();

        // Création initiale d'une Saga en attente de compensation
        Saga saga = Saga.builder()
                .sagaId(sagaId)
                .sagaType(SagaType.CREATE_SALE)
                .saleReference(ref)
                .reservationId(ref)
                .status(SagaStatus.COMPENSATION_PENDING)
                .attemptCount(1)
                .maxAttempts(5)
                .nextRetryAt(LocalDateTime.now().minusMinutes(1)) // Éligible immédiatement
                .build();
        sagaRepository.save(saga);

        // Tentative 1 du worker : product-service renvoie 503
        org.mockito.Mockito.doThrow(new RemoteServiceUnavailableException("Product Service 503 - Tentative 1", HttpStatus.SERVICE_UNAVAILABLE))
                .when(productServiceClient).releaseStock(ref);

        int processed1 = sagaRecoveryWorker.recoverPendingSagas();
        assertThat(processed1).isEqualTo(1);

        Saga afterAttempt1 = sagaRepository.findBySagaId(sagaId).orElseThrow();
        assertThat(afterAttempt1.getStatus()).isEqualTo(SagaStatus.COMPENSATION_PENDING);
        assertThat(afterAttempt1.getAttemptCount()).isEqualTo(2);
        assertThat(afterAttempt1.getLastError()).contains("Product Service 503");

        // Simulation : Le délai de backoff s'est écoulé, et product-service est de nouveau disponible
        afterAttempt1.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        sagaRepository.save(afterAttempt1);

        org.mockito.Mockito.doReturn(new StockReleaseResponse(ref, "RELEASED", "Stock libéré"))
                .when(productServiceClient).releaseStock(ref);

        // Tentative 2 du worker : Succès
        int processed2 = sagaRecoveryWorker.recoverPendingSagas();
        assertThat(processed2).isEqualTo(1);

        Saga afterAttempt2 = sagaRepository.findBySagaId(sagaId).orElseThrow();
        assertThat(afterAttempt2.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(afterAttempt2.getCompletedAt()).isNotNull();

        // Vérification de la piste d'audit des événements
        List<SagaEvent> events = sagaEventRepository.findBySagaIdOrderByCreatedAtAsc(sagaId);
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).getToStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    @DisplayName("Reprise après redémarrage : Les Sagas non complétées en base sont reprises automatiquement sans perte d'état")
    void shouldRecoverPendingSagasFromDatabaseAfterRestart() {
        String sagaId = "SAGA-RESTART-" + randomSuffix();
        String resId = "RES-RESTART-" + randomSuffix();

        // 1. Simuler une Saga persistée avant le crash/redémarrage
        Saga persistedBeforeCrash = Saga.builder()
                .sagaId(sagaId)
                .sagaType(SagaType.CANCEL_SALE)
                .saleReference("REF-" + randomSuffix())
                .reservationId(resId)
                .status(SagaStatus.COMPENSATION_PENDING)
                .attemptCount(0)
                .maxAttempts(5)
                .nextRetryAt(LocalDateTime.now().minusSeconds(5))
                .build();
        sagaRepository.save(persistedBeforeCrash);

        // 2. Simuler le redémarrage : On injecte un nouveau worker ou on invoque recoverPendingSagas
        when(productServiceClient.releaseStock(resId))
                .thenReturn(new StockReleaseResponse(resId, "RELEASED", "Libération post-redémarrage réussie"));

        int recovered = sagaRecoveryWorker.recoverPendingSagas();
        assertThat(recovered).isEqualTo(1);

        // 3. Vérifier que la Saga est maintenant COMPENSATED en base
        Saga recoveredSaga = sagaRepository.findBySagaId(sagaId).orElseThrow();
        assertThat(recoveredSaga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(recoveredSaga.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Concurrence PostgreSQL : FOR UPDATE SKIP LOCKED prévient le double traitement par deux workers concurrents")
    void shouldProcessSagasConcurrentlyWithoutConflictsUsingSkipLocked() throws Exception {
        String sagaId1 = "SAGA-LOCK-1-" + randomSuffix();
        String sagaId2 = "SAGA-LOCK-2-" + randomSuffix();

        Saga s1 = Saga.builder()
                .sagaId(sagaId1)
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-1")
                .reservationId("RES-1")
                .status(SagaStatus.COMPENSATION_PENDING)
                .attemptCount(0)
                .maxAttempts(5)
                .nextRetryAt(LocalDateTime.now().minusMinutes(1))
                .build();

        Saga s2 = Saga.builder()
                .sagaId(sagaId2)
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-2")
                .reservationId("RES-2")
                .status(SagaStatus.COMPENSATION_PENDING)
                .attemptCount(0)
                .maxAttempts(5)
                .nextRetryAt(LocalDateTime.now().minusMinutes(1))
                .build();

        sagaRepository.saveAll(List.of(s1, s2));

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch releaseLockLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Worker 1 : Récupère 1 saga avec FOR UPDATE SKIP LOCKED et conserve le verrou temporairement
        Future<List<String>> worker1Future = executor.submit(() -> txTemplate.execute(status -> {
            List<Saga> batch1 = sagaRepository.findEligibleForRecovery(LocalDateTime.now(), 1);
            lockAcquiredLatch.countDown();
            try {
                // Maintenir le verrou jusqu'à ce que worker 2 exécute sa requête
                releaseLockLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return batch1.stream().map(Saga::getSagaId).toList();
        }));

        lockAcquiredLatch.await(3, TimeUnit.SECONDS);

        // Worker 2 : En parallèle, exécute findEligibleForRecovery(now, 1). Grâce à SKIP LOCKED,
        // il doit ignorer la saga verrouillée par Worker 1 et récupérer l'autre saga sans bloquer !
        Future<List<String>> worker2Future = executor.submit(() -> txTemplate.execute(status -> {
            List<Saga> batch2 = sagaRepository.findEligibleForRecovery(LocalDateTime.now(), 1);
            return batch2.stream().map(Saga::getSagaId).toList();
        }));

        List<String> worker2Results = worker2Future.get(5, TimeUnit.SECONDS);
        releaseLockLatch.countDown();
        List<String> worker1Results = worker1Future.get(5, TimeUnit.SECONDS);

        executor.shutdown();

        assertThat(worker1Results).hasSize(1);
        assertThat(worker2Results).hasSize(1);
        // Les deux workers ne doivent JAMAIS avoir sélectionné la même Saga
        assertThat(worker1Results.get(0)).isNotEqualTo(worker2Results.get(0));
    }
}
