package com.aibi.sales_service.saga.service;

import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.StockReleaseResponse;
import com.aibi.sales_service.exception.RemoteServiceUnavailableException;
import com.aibi.sales_service.saga.entity.Saga;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;
import com.aibi.sales_service.saga.repository.SagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaRecoveryWorkerTest {

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private SagaService sagaService;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private SagaRecoveryWorker recoveryWorker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recoveryWorker, "recoveryEnabled", true);
        ReflectionTestUtils.setField(recoveryWorker, "maxAttempts", 5);
        ReflectionTestUtils.setField(recoveryWorker, "initialBackoffMs", 1000L);
        ReflectionTestUtils.setField(recoveryWorker, "maxBackoffMs", 30000L);
        ReflectionTestUtils.setField(recoveryWorker, "backoffMultiplier", 2.0);
        ReflectionTestUtils.setField(recoveryWorker, "batchSize", 10);
    }

    @Test
    @DisplayName("recoverPendingSagas : Succès de la libération -> Saga marquée COMPENSATED")
    void recoverPendingSagas_Success() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-REC-001")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-REC-001")
                .reservationId("REF-REC-001")
                .status(SagaStatus.COMPENSATION_PENDING)
                .attemptCount(1)
                .maxAttempts(5)
                .build();

        when(sagaRepository.findEligibleForRecovery(any(), eq(10))).thenReturn(List.of(saga));
        when(sagaService.claimSagaForCompensation("SAGA-REC-001")).thenReturn(true);
        when(productServiceClient.releaseStock("REF-REC-001"))
                .thenReturn(new StockReleaseResponse("REF-REC-001", "RELEASED", "Stock libéré"));

        int processed = recoveryWorker.recoverPendingSagas();

        assertThat(processed).isEqualTo(1);
        verify(sagaService).claimSagaForCompensation("SAGA-REC-001");
        verify(productServiceClient).releaseStock("REF-REC-001");
        verify(sagaService).recordCompensationAttempt("SAGA-REC-001", true, null, null);
    }

    @Test
    @DisplayName("recoverPendingSagas : Échec réseau de libération -> Enregistre l'échec et planifie le prochain essai avec backoff")
    void recoverPendingSagas_NetworkFailure() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-REC-002")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-REC-002")
                .reservationId("REF-REC-002")
                .status(SagaStatus.COMPENSATION_PENDING)
                .attemptCount(1)
                .maxAttempts(5)
                .build();

        when(sagaRepository.findEligibleForRecovery(any(), eq(10))).thenReturn(List.of(saga));
        when(sagaService.claimSagaForCompensation("SAGA-REC-002")).thenReturn(true);
        when(productServiceClient.releaseStock("REF-REC-002"))
                .thenThrow(new RemoteServiceUnavailableException("Product Service 503", HttpStatus.SERVICE_UNAVAILABLE));

        int processed = recoveryWorker.recoverPendingSagas();

        assertThat(processed).isEqualTo(1);
        verify(sagaService).recordCompensationAttempt(eq("SAGA-REC-002"), eq(false), contains("Product Service 503"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("calculateNextRetry : Progression exponentielle bornée par maxBackoff avec jitter")
    void calculateNextRetry_ExponentialBackoffProgression() {
        LocalDateTime now = LocalDateTime.now();

        // Tentative 1 : délai base ~ 1000ms
        LocalDateTime retry1 = recoveryWorker.calculateNextRetry(1);
        assertThat(retry1).isAfter(now);

        // Tentative 2 : délai base ~ 2000ms
        LocalDateTime retry2 = recoveryWorker.calculateNextRetry(2);
        assertThat(retry2).isAfter(retry1);

        // Tentative 10 : borné à maxBackoffMs (30 000 ms + max 20% jitter = 36 000 ms max)
        LocalDateTime retry10 = recoveryWorker.calculateNextRetry(10);
        assertThat(retry10).isBeforeOrEqualTo(now.plusSeconds(40));
    }

    @Test
    @DisplayName("scheduledRecovery : Ne fait rien si la reprise est désactivée via configuration")
    void scheduledRecovery_Disabled() {
        ReflectionTestUtils.setField(recoveryWorker, "recoveryEnabled", false);

        recoveryWorker.scheduledRecovery();

        verify(sagaRepository, never()).findEligibleForRecovery(any(), anyInt());
    }
}
