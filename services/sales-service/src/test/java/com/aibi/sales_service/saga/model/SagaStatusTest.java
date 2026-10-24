package com.aibi.sales_service.saga.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStatusTest {

    @Test
    @DisplayName("STARTED : Transitions valides et invalides")
    void startedTransitions() {
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.STOCK_RESERVED)).isTrue();
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.COMPENSATION_PENDING)).isTrue();
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.COMPENSATED)).isTrue();

        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.COMPLETED)).isFalse();
        assertThat(SagaStatus.STARTED.canTransitionTo(SagaStatus.SALE_PERSISTED)).isFalse();
        assertThat(SagaStatus.STARTED.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("STOCK_RESERVED : Transitions valides et invalides")
    void stockReservedTransitions() {
        assertThat(SagaStatus.STOCK_RESERVED.canTransitionTo(SagaStatus.SALE_PERSISTED)).isTrue();
        assertThat(SagaStatus.STOCK_RESERVED.canTransitionTo(SagaStatus.COMPENSATION_PENDING)).isTrue();
        assertThat(SagaStatus.STOCK_RESERVED.canTransitionTo(SagaStatus.COMPENSATING)).isTrue();

        assertThat(SagaStatus.STOCK_RESERVED.canTransitionTo(SagaStatus.COMPLETED)).isFalse();
        assertThat(SagaStatus.STOCK_RESERVED.canTransitionTo(SagaStatus.STARTED)).isFalse();
    }

    @Test
    @DisplayName("SALE_PERSISTED : Transitions valides et invalides")
    void salePersistedTransitions() {
        assertThat(SagaStatus.SALE_PERSISTED.canTransitionTo(SagaStatus.COMPLETED)).isTrue();
        assertThat(SagaStatus.SALE_PERSISTED.canTransitionTo(SagaStatus.COMPENSATION_PENDING)).isTrue();

        assertThat(SagaStatus.SALE_PERSISTED.canTransitionTo(SagaStatus.STOCK_RESERVED)).isFalse();
    }

    @Test
    @DisplayName("COMPLETED : Seule transition autorisée est vers COMPENSATION_PENDING lors d'une annulation")
    void completedTransitions() {
        assertThat(SagaStatus.COMPLETED.canTransitionTo(SagaStatus.COMPENSATION_PENDING)).isTrue();
        assertThat(SagaStatus.COMPLETED.canTransitionTo(SagaStatus.STOCK_RESERVED)).isFalse();
        assertThat(SagaStatus.COMPLETED.canTransitionTo(SagaStatus.COMPENSATED)).isFalse();
    }

    @Test
    @DisplayName("COMPENSATION_PENDING : Transitions valides")
    void compensationPendingTransitions() {
        assertThat(SagaStatus.COMPENSATION_PENDING.canTransitionTo(SagaStatus.COMPENSATING)).isTrue();
        assertThat(SagaStatus.COMPENSATION_PENDING.canTransitionTo(SagaStatus.FAILED_REQUIRES_ATTENTION)).isTrue();
        assertThat(SagaStatus.COMPENSATION_PENDING.canTransitionTo(SagaStatus.COMPENSATED)).isTrue();

        assertThat(SagaStatus.COMPENSATION_PENDING.canTransitionTo(SagaStatus.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("COMPENSATING : Transitions valides")
    void compensatingTransitions() {
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.COMPENSATED)).isTrue();
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.COMPENSATION_PENDING)).isTrue();
        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.FAILED_REQUIRES_ATTENTION)).isTrue();

        assertThat(SagaStatus.COMPENSATING.canTransitionTo(SagaStatus.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("COMPENSATED : État terminal, aucune transition autorisée")
    void compensatedIsTerminal() {
        for (SagaStatus status : SagaStatus.values()) {
            assertThat(SagaStatus.COMPENSATED.canTransitionTo(status)).isFalse();
        }
    }

    @Test
    @DisplayName("FAILED_REQUIRES_ATTENTION : Permet réessai manuel (COMPENSATION_PENDING) ou résolution (COMPENSATED)")
    void failedRequiresAttentionTransitions() {
        assertThat(SagaStatus.FAILED_REQUIRES_ATTENTION.canTransitionTo(SagaStatus.COMPENSATION_PENDING)).isTrue();
        assertThat(SagaStatus.FAILED_REQUIRES_ATTENTION.canTransitionTo(SagaStatus.COMPENSATED)).isTrue();

        assertThat(SagaStatus.FAILED_REQUIRES_ATTENTION.canTransitionTo(SagaStatus.COMPLETED)).isFalse();
        assertThat(SagaStatus.FAILED_REQUIRES_ATTENTION.canTransitionTo(SagaStatus.STARTED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SagaStatus.class)
    @DisplayName("Toute transition vers null retourne false")
    void transitionsToNullAlwaysFalse(SagaStatus status) {
        assertThat(status.canTransitionTo(null)).isFalse();
    }
}
