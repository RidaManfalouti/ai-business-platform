package com.aibi.analytics_service.service;

import com.aibi.analytics_service.client.CustomerServiceClient;
import com.aibi.analytics_service.client.ProductServiceClient;
import com.aibi.analytics_service.client.SalesServiceClient;
import com.aibi.analytics_service.client.dto.*;
import com.aibi.analytics_service.dto.*;
import com.aibi.analytics_service.exception.DownstreamServiceException;
import com.aibi.analytics_service.exception.InvalidDateRangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SalesServiceClient salesServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private CustomerServiceClient customerServiceClient;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                salesServiceClient,
                productServiceClient,
                customerServiceClient,
                "EUR",
                "UTC",
                5
        );
    }

    @Test
    @DisplayName("Calcul des KPIs avec des données réelles")
    void testGetKpiSummary_Success() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 27);

        when(salesServiceClient.getSummaryAggregate(any(), any()))
                .thenReturn(new SalesSummaryDto(new BigDecimal("10000.00"), 20L, 25L));
        when(customerServiceClient.getTotalCustomersCount()).thenReturn(50L);
        when(productServiceClient.getTotalProductsCount()).thenReturn(100L);

        KpiSummaryResponse response = analyticsService.getKpiSummary(from, to, "UTC");

        assertThat(response).isNotNull();
        assertThat(response.totalRevenue()).isEqualByComparingTo("10000.00");
        assertThat(response.completedSalesCount()).isEqualTo(20L);
        assertThat(response.totalSalesCount()).isEqualTo(25L);
        assertThat(response.averageOrderValue()).isEqualByComparingTo("500.00");
        assertThat(response.totalCustomers()).isEqualTo(50L);
        assertThat(response.totalProducts()).isEqualTo(100L);
        assertThat(response.currency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Calcul des KPIs lorsque aucune vente n'est complétée (panier moyen = 0)")
    void testGetKpiSummary_ZeroCompletedSales() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 27);

        when(salesServiceClient.getSummaryAggregate(any(), any()))
                .thenReturn(new SalesSummaryDto(BigDecimal.ZERO, 0L, 5L));
        when(customerServiceClient.getTotalCustomersCount()).thenReturn(10L);
        when(productServiceClient.getTotalProductsCount()).thenReturn(20L);

        KpiSummaryResponse response = analyticsService.getKpiSummary(from, to, "UTC");

        assertThat(response.totalRevenue()).isEqualByComparingTo("0.00");
        assertThat(response.completedSalesCount()).isEqualTo(0L);
        assertThat(response.averageOrderValue()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Calcul des KPIs avec données vides / nulles du service des ventes")
    void testGetKpiSummary_NullSalesSummary() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 27);

        when(salesServiceClient.getSummaryAggregate(any(), any())).thenReturn(null);
        when(customerServiceClient.getTotalCustomersCount()).thenReturn(0L);
        when(productServiceClient.getTotalProductsCount()).thenReturn(0L);

        KpiSummaryResponse response = analyticsService.getKpiSummary(from, to, "UTC");

        assertThat(response.totalRevenue()).isEqualByComparingTo("0.00");
        assertThat(response.completedSalesCount()).isEqualTo(0L);
        assertThat(response.averageOrderValue()).isEqualByComparingTo("0.00");
        assertThat(response.totalSalesCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Validation de plage : date de début postérieure à la date de fin -> exception")
    void testDateRange_FromAfterTo() {
        LocalDate from = LocalDate.of(2026, 10, 28);
        LocalDate to = LocalDate.of(2026, 10, 20);

        assertThatThrownBy(() -> analyticsService.getKpiSummary(from, to, "UTC"))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("antérieure ou égale à la date de fin");
    }

    @Test
    @DisplayName("Validation de plage : plage excédant 365 jours -> exception")
    void testDateRange_Exceeds365Days() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> analyticsService.getKpiSummary(from, to, "UTC"))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("dépasse la limite autorisée de 365 jours");
    }

    @Test
    @DisplayName("Validation du fuseau horaire : fuseau invalide -> exception")
    void testInvalidTimezone() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 27);

        assertThatThrownBy(() -> analyticsService.getKpiSummary(from, to, "Invalid/TimeZone_404"))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessageContaining("Fuseau horaire invalide");
    }

    @Test
    @DisplayName("Plage de date sur un seul jour (from == to)")
    void testSingleDayRange() {
        LocalDate day = LocalDate.of(2026, 10, 25);
        when(salesServiceClient.getSummaryAggregate(any(), any()))
                .thenReturn(new SalesSummaryDto(new BigDecimal("150.00"), 1L, 1L));

        KpiSummaryResponse response = analyticsService.getKpiSummary(day, day, "UTC");

        assertThat(response).isNotNull();
        assertThat(response.totalRevenue()).isEqualByComparingTo("150.00");
        assertThat(response.completedSalesCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Récupération des tendances quotidiennes")
    void testGetTrends() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 2);

        when(salesServiceClient.getDailyTrends(any(), any())).thenReturn(List.of(
                new DailyTrendDto(LocalDate.of(2026, 10, 1), 5L, new BigDecimal("1200.00")),
                new DailyTrendDto(LocalDate.of(2026, 10, 2), 3L, new BigDecimal("800.50"))
        ));

        TrendsResponse trends = analyticsService.getTrends(from, to, "UTC");

        assertThat(trends.trends()).hasSize(2);
        assertThat(trends.totalPeriodRevenue()).isEqualByComparingTo("2000.50");
        assertThat(trends.totalPeriodOrders()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Classement des meilleures ventes")
    void testGetTopProducts() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 27);

        when(salesServiceClient.getTopProducts(any(), any(), anyInt())).thenReturn(List.of(
                new TopProductDto(1L, "ThinkPad X1", 10L, new BigDecimal("15000.00")),
                new TopProductDto(2L, "Souris Sans Fil", 25L, new BigDecimal("750.00"))
        ));

        TopProductsResponse top = analyticsService.getTopProducts(from, to, "UTC", 5);

        assertThat(top.items()).hasSize(2);
        assertThat(top.totalQuantitySold()).isEqualTo(35L);
        assertThat(top.totalRevenue()).isEqualByComparingTo("15750.00");
    }

    @Test
    @DisplayName("Distribution des statuts de commande avec pourcentages")
    void testGetStatusDistribution() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 27);

        when(salesServiceClient.getStatusDistribution(any(), any())).thenReturn(List.of(
                new SalesStatusCountDto("COMPLETED", 60L),
                new SalesStatusCountDto("PENDING", 20L),
                new SalesStatusCountDto("CONFIRMED", 10L),
                new SalesStatusCountDto("CANCELLED", 10L)
        ));

        StatusDistributionResponse response = analyticsService.getStatusDistribution(from, to, "UTC");

        assertThat(response.totalOrders()).isEqualTo(100L);
        assertThat(response.distribution()).hasSize(4);

        StatusDistributionItem completed = response.distribution().stream()
                .filter(d -> d.status().equals("COMPLETED"))
                .findFirst()
                .orElseThrow();
        assertThat(completed.count()).isEqualTo(60L);
        assertThat(completed.percentage()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Alertes de stock faible et détection de rupture de stock")
    void testGetLowStock() {
        when(productServiceClient.getProductsSortedByStock(10)).thenReturn(new ProductPageDto(
                List.of(
                        new ProductItemDto(1L, "Clé USB", "Stock épuisé", new BigDecimal("15.00"), 0, "Accessoire"),
                        new ProductItemDto(2L, "Écran 4K", "Stock critique", new BigDecimal("400.00"), 3, "Écran"),
                        new ProductItemDto(3L, "Serveur", "Stock normal", new BigDecimal("2000.00"), 20, "Serveur")
                ),
                3L, 1, 10, 0
        ));

        LowStockResponse lowStock = analyticsService.getLowStock(5, 10);

        assertThat(lowStock.items()).hasSize(2);
        assertThat(lowStock.items().get(0).stockStatus()).isEqualTo("OUT_OF_STOCK");
        assertThat(lowStock.items().get(1).stockStatus()).isEqualTo("LOW_STOCK");
    }

    @Test
    @DisplayName("Propagation de l'erreur downstream quand sales-service est indisponible (503)")
    void testDownstreamFailure_SalesService503() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 27);

        when(salesServiceClient.getSummaryAggregate(any(), any()))
                .thenThrow(new DownstreamServiceException("Sales service unavailable", HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> analyticsService.getKpiSummary(from, to, "UTC"))
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessageContaining("Sales service unavailable");
    }
}
