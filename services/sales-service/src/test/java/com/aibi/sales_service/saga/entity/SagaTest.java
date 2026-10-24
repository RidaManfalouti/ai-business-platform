package com.aibi.sales_service.saga.entity;

import com.aibi.sales_service.saga.exception.InvalidSagaStateTransitionException;
import com.aibi.sales_service.saga.model.SagaStatus;
import com.aibi.sales_service.saga.model.SagaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaTest {

    @Test
    @DisplayName("transitionTo : Transition valide met à jour le statut et completedAt pour COMPLETED")
    void transitionTo_ValidTransition() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-001")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-001")
                .status(SagaStatus.STARTED)
                .build();

        saga.transitionTo(SagaStatus.STOCK_RESERVED, "Stock réservé");
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STOCK_RESERVED);
        assertThat(saga.getUpdatedAt()).isNotNull();

        saga.transitionTo(SagaStatus.SALE_PERSISTED, "Vente persistée");
        saga.transitionTo(SagaStatus.COMPLETED, "Terminé");
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("transitionTo : Transition invalide lève InvalidSagaStateTransitionException")
    void transitionTo_InvalidTransitionThrows() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-002")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-002")
                .status(SagaStatus.STARTED)
                .build();

        assertThatThrownBy(() -> saga.transitionTo(SagaStatus.COMPLETED, "Illegal jump"))
                .isInstanceOf(InvalidSagaStateTransitionException.class)
                .hasMessageContaining("impossible de passer de STARTED à COMPLETED");
    }

    @Test
    @DisplayName("recordAttempt : Succès de compensation -> passe à COMPENSATED et vide nextRetryAt")
    void recordAttempt_Success() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-003")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-003")
                .status(SagaStatus.COMPENSATING)
                .attemptCount(1)
                .maxAttempts(5)
                .build();

        saga.recordAttempt(true, null, null);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATED);
        assertThat(saga.getAttemptCount()).isEqualTo(2);
        assertThat(saga.getNextRetryAt()).isNull();
        assertThat(saga.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("recordAttempt : Échec avec tentatives restantes -> COMPENSATION_PENDING avec nextRetryAt")
    void recordAttempt_FailureWithRetriesRemaining() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-004")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-004")
                .status(SagaStatus.COMPENSATING)
                .attemptCount(1)
                .maxAttempts(5)
                .build();

        LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(10);
        saga.recordAttempt(false, "Timeout réseau", nextRetry);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPENSATION_PENDING);
        assertThat(saga.getAttemptCount()).isEqualTo(2);
        assertThat(saga.getNextRetryAt()).isEqualTo(nextRetry);
        assertThat(saga.getLastError()).isEqualTo("Timeout réseau");
    }

    @Test
    @DisplayName("recordAttempt : Épuisement des tentatives -> FAILED_REQUIRES_ATTENTION")
    void recordAttempt_MaxAttemptsExhausted() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-005")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-005")
                .status(SagaStatus.COMPENSATING)
                .attemptCount(4)
                .maxAttempts(5)
                .build();

        LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(10);
        saga.recordAttempt(false, "Erreur 500 persistante", nextRetry);

        assertThat(saga.getStatus()).isEqualTo(SagaStatus.FAILED_REQUIRES_ATTENTION);
        assertThat(saga.getAttemptCount()).isEqualTo(5);
        assertThat(saga.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("recordAttempt : Assainissement strict des erreurs sensibles (masquage de tokens, passwords, secrets)")
    void recordAttempt_SanitizesSensitiveData() {
        Saga saga = Saga.builder()
                .sagaId("SAGA-006")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-006")
                .status(SagaStatus.COMPENSATING)
                .attemptCount(0)
                .maxAttempts(5)
                .build();

        String rawError = "Erreur avec Bearer eyJhbGciOiJIUzI1NiJ9.secretToken123 et password=SuperSecretPassword!";
        saga.recordAttempt(false, rawError, LocalDateTime.now().plusSeconds(5));

        assertThat(saga.getLastError()).doesNotContain("secretToken123");
        assertThat(saga.getLastError()).doesNotContain("SuperSecretPassword!");
        assertThat(saga.getLastError()).contains("Bearer ***");
        assertThat(saga.getLastError()).contains("password=***");
    }
}
