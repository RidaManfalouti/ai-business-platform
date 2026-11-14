package com.aibi.ai_service.tools;

import com.aibi.ai_service.exception.InvalidToolArgumentException;
import com.aibi.ai_service.tools.client.AnalyticsClient;
import com.aibi.ai_service.tools.client.CustomerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessToolExecutorTest {

    @Mock
    private AnalyticsClient analyticsClient;

    @Mock
    private CustomerClient customerClient;

    private BusinessToolExecutor toolExecutor;

    @BeforeEach
    void setUp() {
        toolExecutor = new BusinessToolExecutor(analyticsClient, customerClient, new ObjectMapper());
    }

    @Test
    @DisplayName("getSalesSummary avec dates valides -> exécute AnalyticsClient avec succès")
    void testGetSalesSummary_ValidDates() {
        when(analyticsClient.getKpiSummary("2026-10-01", "2026-10-31", "UTC"))
                .thenReturn("{\"totalRevenue\":5000.00}");

        String result = toolExecutor.executeTool("getSalesSummary", "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"timezone\":\"UTC\"}");

        assertEquals("{\"totalRevenue\":5000.00}", result);
        verify(analyticsClient).getKpiSummary("2026-10-01", "2026-10-31", "UTC");
    }

    @Test
    @DisplayName("Format de date invalide (injection de commandes ou mauvais format) -> lève InvalidToolArgumentException")
    void testInvalidDateFormat_ThrowsException() {
        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getSalesSummary", "{\"from\":\"2026/10/01; DROP TABLE users;\"}"));

        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getSalesSummary", "{\"from\":\"invalid-date\"}"));
    }

    @Test
    @DisplayName("Paramètre limit négatif ou excessif -> lève InvalidToolArgumentException")
    void testOutOfBoundsLimit_ThrowsException() {
        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getTopSellingProducts", "{\"limit\":-1}"));

        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getTopSellingProducts", "{\"limit\":500}"));
    }

    @Test
    @DisplayName("getLowStockProducts avec seuil valide -> exécute AnalyticsClient")
    void testGetLowStockProducts_Valid() {
        when(analyticsClient.getLowStock(5, 10))
                .thenReturn("{\"items\":[]}");

        String result = toolExecutor.executeTool("getLowStockProducts", "{\"threshold\":5,\"limit\":10}");

        assertEquals("{\"items\":[]}", result);
        verify(analyticsClient).getLowStock(5, 10);
    }

    @Test
    @DisplayName("Outil inconnu -> lève InvalidToolArgumentException")
    void testUnknownTool_ThrowsException() {
        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("executeArbitrarySql", "{\"sql\":\"SELECT *\"}"));
    }

    @Test
    @DisplayName("compareRevenuePeriods avec deux périodes valides -> exécute les deux requêtes")
    void testCompareRevenuePeriods_Valid() {
        when(analyticsClient.getKpiSummary("2026-09-01", "2026-09-30", "UTC"))
                .thenReturn("{\"revenue\":4000}");
        when(analyticsClient.getKpiSummary("2026-10-01", "2026-10-31", "UTC"))
                .thenReturn("{\"revenue\":6000}");

        String result = toolExecutor.executeTool("compareRevenuePeriods",
                "{\"period1From\":\"2026-09-01\",\"period1To\":\"2026-09-30\",\"period2From\":\"2026-10-01\",\"period2To\":\"2026-10-31\"}");

        assertTrue(result.contains("\"period1\":{\"revenue\":4000}"));
        assertTrue(result.contains("\"period2\":{\"revenue\":6000}"));
    }
}
