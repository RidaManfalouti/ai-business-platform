package com.aibi.analytics_service.controller;

import com.aibi.analytics_service.dto.*;
import com.aibi.analytics_service.exception.DownstreamServiceException;
import com.aibi.analytics_service.exception.InvalidDateRangeException;
import com.aibi.analytics_service.security.JwtRoleConverter;
import com.aibi.analytics_service.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JwtRoleConverter.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("GET /api/analytics/kpi retourne les indicateurs clés")
    void testGetKpi() throws Exception {
        when(analyticsService.getKpiSummary(any(), any(), any()))
                .thenReturn(new KpiSummaryResponse(
                        new BigDecimal("25000.00"),
                        50L,
                        new BigDecimal("500.00"),
                        60L,
                        120L,
                        35L,
                        "EUR",
                        Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-27T23:59:59Z"),
                        "UTC"
                ));

        mockMvc.perform(get("/api/analytics/kpi")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-27")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(25000.00))
                .andExpect(jsonPath("$.completedSalesCount").value(50))
                .andExpect(jsonPath("$.averageOrderValue").value(500.00))
                .andExpect(jsonPath("$.totalCustomers").value(120))
                .andExpect(jsonPath("$.totalProducts").value(35))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("GET /api/analytics/kpi avec plage invalide retourne 400 Bad Request")
    void testGetKpi_InvalidRange() throws Exception {
        when(analyticsService.getKpiSummary(any(), any(), any()))
                .thenThrow(new InvalidDateRangeException("La date de début ne peut être postérieure à la date de fin."));

        mockMvc.perform(get("/api/analytics/kpi")
                        .param("from", "2026-10-28")
                        .param("to", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("La date de début ne peut être postérieure à la date de fin."));
    }

    @Test
    @DisplayName("GET /api/analytics/kpi lors d'une indisponibilité downstream retourne 503")
    void testGetKpi_DownstreamUnavailable() throws Exception {
        when(analyticsService.getKpiSummary(any(), any(), any()))
                .thenThrow(new DownstreamServiceException("Service des ventes indisponible", HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(get("/api/analytics/kpi"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Service des ventes indisponible"));
    }

    @Test
    @DisplayName("GET /api/analytics/trends retourne les tendances quotidiennes")
    void testGetTrends() throws Exception {
        when(analyticsService.getTrends(any(), any(), any()))
                .thenReturn(new TrendsResponse(
                        List.of(new RevenueTrendPoint(LocalDate.of(2026, 10, 25), 5L, new BigDecimal("2500.00"), "EUR")),
                        new BigDecimal("2500.00"),
                        5L,
                        "EUR"
                ));

        mockMvc.perform(get("/api/analytics/trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends[0].date").value("2026-10-25"))
                .andExpect(jsonPath("$.trends[0].revenue").value(2500.00))
                .andExpect(jsonPath("$.totalPeriodRevenue").value(2500.00));
    }

    @Test
    @DisplayName("GET /api/analytics/top-products retourne le top produits")
    void testGetTopProducts() throws Exception {
        when(analyticsService.getTopProducts(any(), any(), any(), eq(5)))
                .thenReturn(new TopProductsResponse(
                        List.of(new TopProductItem(1L, "Laptop", 10L, new BigDecimal("12000.00"), "EUR")),
                        10L,
                        new BigDecimal("12000.00"),
                        "EUR"
                ));

        mockMvc.perform(get("/api/analytics/top-products?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.items[0].quantitySold").value(10))
                .andExpect(jsonPath("$.totalRevenue").value(12000.00));
    }

    @Test
    @DisplayName("GET /api/analytics/status-distribution retourne la répartition")
    void testGetStatusDistribution() throws Exception {
        when(analyticsService.getStatusDistribution(any(), any(), any()))
                .thenReturn(new StatusDistributionResponse(
                        List.of(new StatusDistributionItem("COMPLETED", 50L, 100.0)),
                        50L
                ));

        mockMvc.perform(get("/api/analytics/status-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribution[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.distribution[0].percentage").value(100.0));
    }

    @Test
    @DisplayName("GET /api/analytics/low-stock retourne les alertes d'inventaire")
    void testGetLowStock() throws Exception {
        when(analyticsService.getLowStock(eq(5), eq(10)))
                .thenReturn(new LowStockResponse(
                        List.of(new LowStockItem(1L, "Clé USB", "Accessoire", new BigDecimal("15.00"), 2, "LOW_STOCK")),
                        1L,
                        5
                ));

        mockMvc.perform(get("/api/analytics/low-stock?threshold=5&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Clé USB"))
                .andExpect(jsonPath("$.items[0].stockQuantity").value(2))
                .andExpect(jsonPath("$.threshold").value(5));
    }

    @Test
    @DisplayName("GET /api/analytics/dashboard retourne le payload complet")
    void testGetDashboard() throws Exception {
        KpiSummaryResponse kpi = new KpiSummaryResponse(new BigDecimal("1000.00"), 2L, new BigDecimal("500.00"), 2L, 5L, 10L, "EUR", Instant.now(), Instant.now(), "UTC");
        TrendsResponse trends = new TrendsResponse(List.of(), BigDecimal.ZERO, 0L, "EUR");
        TopProductsResponse topProducts = new TopProductsResponse(List.of(), 0L, BigDecimal.ZERO, "EUR");
        StatusDistributionResponse statusDistribution = new StatusDistributionResponse(List.of(), 0L);
        LowStockResponse lowStock = new LowStockResponse(List.of(), 0L, 5);

        when(analyticsService.getDashboard(any(), any(), any(), any(), any()))
                .thenReturn(new DashboardAnalyticsResponse(kpi, trends, topProducts, statusDistribution, lowStock));

        mockMvc.perform(get("/api/analytics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpi.totalRevenue").value(1000.00))
                .andExpect(jsonPath("$.trends").exists())
                .andExpect(jsonPath("$.topProducts").exists())
                .andExpect(jsonPath("$.statusDistribution").exists())
                .andExpect(jsonPath("$.lowStock").exists());
    }
}
