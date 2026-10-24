package com.aibi.sales_service.reconciliation.service;

import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.StockReservationResponse;
import com.aibi.sales_service.entity.Sale;
import com.aibi.sales_service.model.SaleStatus;
import com.aibi.sales_service.reconciliation.dto.ReconciliationReport;
import com.aibi.sales_service.reconciliation.model.DiscrepancyType;
import com.aibi.sales_service.repository.SaleRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReconciliationServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SagaRepository sagaRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private StockReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reconciliationService, "reconciliationEnabled", true);
        ReflectionTestUtils.setField(reconciliationService, "staleThresholdMinutes", 15);
    }

    @Test
    @DisplayName("runReconciliation : Détecte les Sagas bloquées en FAILED_REQUIRES_ATTENTION sans altérer les entités")
    void runReconciliation_DetectsAttentionRequiredSagas() {
        Saga failedSaga = Saga.builder()
                .sagaId("SAGA-FAIL-01")
                .sagaType(SagaType.CREATE_SALE)
                .saleReference("REF-FAIL-01")
                .reservationId("REF-FAIL-01")
                .status(SagaStatus.FAILED_REQUIRES_ATTENTION)
                .attemptCount(5)
                .maxAttempts(5)
                .lastError("Timeout répété")
                .build();

        when(sagaRepository.findByStatus(SagaStatus.FAILED_REQUIRES_ATTENTION))
                .thenReturn(List.of(failedSaga));
        when(sagaRepository.findStaleSagas(any(), any())).thenReturn(Collections.emptyList());
        when(saleRepository.findByStatus(eq(SaleStatus.CANCELLED), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        ReconciliationReport report = reconciliationService.runReconciliation(Duration.ofMinutes(15));

        assertThat(report.discrepanciesCount()).isEqualTo(1);
        assertThat(report.discrepancies().get(0).type()).isEqualTo(DiscrepancyType.SAGA_REQUIRES_MANUAL_ATTENTION);
        assertThat(report.discrepancies().get(0).saleReference()).isEqualTo("REF-FAIL-01");

        // Vérification de sécurité absolue : Aucune écriture ni modification de stock
        verify(saleRepository, never()).save(any());
        verify(productServiceClient, never()).releaseStock(any());
    }

    @Test
    @DisplayName("runReconciliation : Détecte une vente CANCELLED dont la réservation est restée active dans Product Service")
    void runReconciliation_DetectsCancelledSaleWithActiveReservation() {
        Sale cancelledSale = Sale.builder()
                .id(10L)
                .reference("SAL-CANCELLED-STALE")
                .status(SaleStatus.CANCELLED)
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();

        when(sagaRepository.findByStatus(SagaStatus.FAILED_REQUIRES_ATTENTION)).thenReturn(Collections.emptyList());
        when(sagaRepository.findStaleSagas(any(), any())).thenReturn(Collections.emptyList());
        when(saleRepository.findByStatus(eq(SaleStatus.CANCELLED), any())).thenReturn(new PageImpl<>(List.of(cancelledSale)));

        StockReservationResponse remoteRes = new StockReservationResponse(
                "SAL-CANCELLED-STALE",
                "RESERVED",
                List.of(),
                LocalDateTime.now().minusMinutes(30)
        );
        when(productServiceClient.getReservation("SAL-CANCELLED-STALE")).thenReturn(remoteRes);

        ReconciliationReport report = reconciliationService.runReconciliation(Duration.ofMinutes(15));

        assertThat(report.discrepanciesCount()).isEqualTo(1);
        assertThat(report.discrepancies().get(0).type()).isEqualTo(DiscrepancyType.CANCELLED_SALE_RESERVATION_STILL_RESERVED);
        assertThat(report.discrepancies().get(0).suggestedAction()).contains("POST /api/products/reservations/SAL-CANCELLED-STALE/release");
    }

    @Test
    @DisplayName("runReconciliation : Aucun écart si le système est intègre")
    void runReconciliation_ZeroDiscrepanciesWhenConsistent() {
        when(sagaRepository.findByStatus(SagaStatus.FAILED_REQUIRES_ATTENTION)).thenReturn(Collections.emptyList());
        when(sagaRepository.findStaleSagas(any(), any())).thenReturn(Collections.emptyList());
        when(saleRepository.findByStatus(eq(SaleStatus.CANCELLED), any())).thenReturn(new PageImpl<>(Collections.emptyList()));

        ReconciliationReport report = reconciliationService.runReconciliation(Duration.ofMinutes(15));

        assertThat(report.discrepanciesCount()).isEqualTo(0);
        assertThat(report.discrepancies()).isEmpty();
    }
}
