package com.aibi.sales_service.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaleStatusTest {

    @Test
    @DisplayName("Vérifie les transitions autorisées depuis PENDING")
    void shouldAllowTransitionsFromPending() {
        assertThat(SaleStatus.PENDING.canTransitionTo(SaleStatus.CONFIRMED)).isTrue();
        assertThat(SaleStatus.PENDING.canTransitionTo(SaleStatus.CANCELLED)).isTrue();
        assertThat(SaleStatus.PENDING.canTransitionTo(SaleStatus.COMPLETED)).isFalse();
        assertThat(SaleStatus.PENDING.canTransitionTo(SaleStatus.PENDING)).isFalse();
        assertThat(SaleStatus.PENDING.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("Vérifie les transitions autorisées depuis CONFIRMED")
    void shouldAllowTransitionsFromConfirmed() {
        assertThat(SaleStatus.CONFIRMED.canTransitionTo(SaleStatus.COMPLETED)).isTrue();
        assertThat(SaleStatus.CONFIRMED.canTransitionTo(SaleStatus.CANCELLED)).isTrue();
        assertThat(SaleStatus.CONFIRMED.canTransitionTo(SaleStatus.PENDING)).isFalse();
        assertThat(SaleStatus.CONFIRMED.canTransitionTo(SaleStatus.CONFIRMED)).isFalse();
        assertThat(SaleStatus.CONFIRMED.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("Vérifie qu'aucun changement n'est possible depuis COMPLETED (état terminal)")
    void shouldDisallowTransitionsFromCompleted() {
        assertThat(SaleStatus.COMPLETED.canTransitionTo(SaleStatus.PENDING)).isFalse();
        assertThat(SaleStatus.COMPLETED.canTransitionTo(SaleStatus.CONFIRMED)).isFalse();
        assertThat(SaleStatus.COMPLETED.canTransitionTo(SaleStatus.CANCELLED)).isFalse();
        assertThat(SaleStatus.COMPLETED.canTransitionTo(SaleStatus.COMPLETED)).isFalse();
        assertThat(SaleStatus.COMPLETED.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("Vérifie qu'aucun changement n'est possible depuis CANCELLED (état terminal)")
    void shouldDisallowTransitionsFromCancelled() {
        assertThat(SaleStatus.CANCELLED.canTransitionTo(SaleStatus.PENDING)).isFalse();
        assertThat(SaleStatus.CANCELLED.canTransitionTo(SaleStatus.CONFIRMED)).isFalse();
        assertThat(SaleStatus.CANCELLED.canTransitionTo(SaleStatus.COMPLETED)).isFalse();
        assertThat(SaleStatus.CANCELLED.canTransitionTo(SaleStatus.CANCELLED)).isFalse();
        assertThat(SaleStatus.CANCELLED.canTransitionTo(null)).isFalse();
    }
}
