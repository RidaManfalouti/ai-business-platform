package com.aibi.ai_service.tools;

import com.aibi.ai_service.exception.InvalidToolArgumentException;
import com.aibi.ai_service.exception.UnauthorizedAccessException;
import com.aibi.ai_service.tools.client.AnalyticsClient;
import com.aibi.ai_service.tools.client.CustomerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessToolExecutorTest {

    @Mock
    private AnalyticsClient analyticsClient;

    @Mock
    private CustomerClient customerClient;

    private BusinessToolAuthorizationService authorizationService;
    private BusinessToolExecutor toolExecutor;

    @BeforeEach
    void setUp() {
        authorizationService = new BusinessToolAuthorizationService();
        toolExecutor = new BusinessToolExecutor(analyticsClient, customerClient, authorizationService, new ObjectMapper());
    }

    @Test
    @DisplayName("getSalesSummary avec dates valides et rôle OWNER -> exécute AnalyticsClient avec succès")
    void testGetSalesSummary_ValidDates() {
        when(analyticsClient.getKpiSummary("2026-10-01", "2026-10-31", "UTC"))
                .thenReturn("{\"totalRevenue\":5000.00}");

        String result = toolExecutor.executeTool("getSalesSummary", "{\"from\":\"2026-10-01\",\"to\":\"2026-10-31\",\"timezone\":\"UTC\"}", "OWNER");

        assertEquals("{\"totalRevenue\":5000.00}", result);
        verify(analyticsClient).getKpiSummary("2026-10-01", "2026-10-31", "UTC");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELLER", "VIEWER", "ANONYMOUS", "GUEST"})
    @DisplayName("CRIT-03 : getSalesSummary avec rôles restreints -> lève UnauthorizedAccessException")
    void testGetSalesSummary_RestrictedRoles_Denied(String role) {
        assertThrows(UnauthorizedAccessException.class, () ->
                toolExecutor.executeTool("getSalesSummary", "{}", role));
        verifyNoInteractions(analyticsClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELLER", "VIEWER"})
    @DisplayName("CRIT-03 : getTopCustomers avec rôles SELLER et VIEWER -> lève UnauthorizedAccessException")
    void testGetTopCustomers_RestrictedRoles_Denied(String role) {
        assertThrows(UnauthorizedAccessException.class, () ->
                toolExecutor.executeTool("getTopCustomers", "{}", role));
        verifyNoInteractions(analyticsClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN", "MANAGER"})
    @DisplayName("CRIT-03 : getTopCustomers avec rôles autorisés -> exécute AnalyticsClient")
    void testGetTopCustomers_AuthorizedRoles_Allowed(String role) {
        when(analyticsClient.getTopCustomers(null, null, null, 10))
                .thenReturn("{\"topCustomers\":[{\"customerId\":1,\"customerName\":\"Société Générale\",\"completedSalesCount\":5,\"totalSpent\":15000.00,\"currency\":\"EUR\"}],\"totalCount\":1}");

        String result = toolExecutor.executeTool("getTopCustomers", "{}", role);

        assertNotNull(result);
        assertTrue(result.contains("Société Générale"));
        assertTrue(result.contains("15000.00"));
        // Assert absence of sensitive customer PII
        assertFalse(result.contains("email"));
        assertFalse(result.contains("phone"));
        assertFalse(result.contains("address"));
        verify(analyticsClient).getTopCustomers(null, null, null, 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN", "MANAGER", "SELLER", "VIEWER"})
    @DisplayName("CRIT-03 : getLowStockProducts autorisé pour tous les rôles authentifiés")
    void testGetLowStockProducts_AllRoles_Allowed(String role) {
        when(analyticsClient.getLowStock(5, 10))
                .thenReturn("{\"items\":[]}");

        String result = toolExecutor.executeTool("getLowStockProducts", "{\"threshold\":5,\"limit\":10}", role);

        assertEquals("{\"items\":[]}", result);
        verify(analyticsClient).getLowStock(5, 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN", "MANAGER", "SELLER", "VIEWER"})
    @DisplayName("CRIT-03 : getTopSellingProducts autorisé pour tous les rôles authentifiés")
    void testGetTopSellingProducts_AllRoles_Allowed(String role) {
        when(analyticsClient.getTopProducts(null, null, null, 5))
                .thenReturn("{\"products\":[]}");

        String result = toolExecutor.executeTool("getTopSellingProducts", "{\"limit\":5}", role);

        assertEquals("{\"products\":[]}", result);
        verify(analyticsClient).getTopProducts(null, null, null, 5);
    }

    @Test
    @DisplayName("CRIT-03 : Rôle null ou vide -> lève UnauthorizedAccessException par défaut (fail-closed)")
    void testNullRole_Denied() {
        assertThrows(UnauthorizedAccessException.class, () ->
                toolExecutor.executeTool("getSalesSummary", "{}", null));

        assertThrows(UnauthorizedAccessException.class, () ->
                toolExecutor.executeTool("getLowStockProducts", "{}", ""));
    }

    @Test
    @DisplayName("Format de date invalide (injection de commandes ou mauvais format) -> lève InvalidToolArgumentException")
    void testInvalidDateFormat_ThrowsException() {
        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getSalesSummary", "{\"from\":\"2026/10/01; DROP TABLE users;\"}", "OWNER"));

        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getSalesSummary", "{\"from\":\"invalid-date\"}", "OWNER"));
    }

    @Test
    @DisplayName("Paramètre limit négatif ou excessif -> lève InvalidToolArgumentException")
    void testOutOfBoundsLimit_ThrowsException() {
        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getTopSellingProducts", "{\"limit\":-1}", "OWNER"));

        assertThrows(InvalidToolArgumentException.class, () ->
                toolExecutor.executeTool("getTopSellingProducts", "{\"limit\":500}", "OWNER"));
    }

    @Test
    @DisplayName("Outil inconnu -> rejeté par l'autorisation avec UnauthorizedAccessException")
    void testUnknownTool_ThrowsException() {
        assertThrows(UnauthorizedAccessException.class, () ->
                toolExecutor.executeTool("executeArbitrarySql", "{\"sql\":\"SELECT *\"}", "OWNER"));
    }

    @Test
    @DisplayName("compareRevenuePeriods avec deux périodes valides et rôle OWNER -> exécute les deux requêtes")
    void testCompareRevenuePeriods_Valid() {
        when(analyticsClient.getKpiSummary("2026-09-01", "2026-09-30", "UTC"))
                .thenReturn("{\"revenue\":4000}");
        when(analyticsClient.getKpiSummary("2026-10-01", "2026-10-31", "UTC"))
                .thenReturn("{\"revenue\":6000}");

        String result = toolExecutor.executeTool("compareRevenuePeriods",
                "{\"period1From\":\"2026-09-01\",\"period1To\":\"2026-09-30\",\"period2From\":\"2026-10-01\",\"period2To\":\"2026-10-31\"}",
                "OWNER");

        assertTrue(result.contains("\"period1\":{\"revenue\":4000}"));
        assertTrue(result.contains("\"period2\":{\"revenue\":6000}"));
    }

    @Test
    @DisplayName("CRIT-03 : compareRevenuePeriods refusé pour le rôle SELLER")
    void testCompareRevenuePeriods_Seller_Denied() {
        assertThrows(UnauthorizedAccessException.class, () ->
                toolExecutor.executeTool("compareRevenuePeriods",
                        "{\"period1From\":\"2026-09-01\",\"period1To\":\"2026-09-30\",\"period2From\":\"2026-10-01\",\"period2To\":\"2026-10-31\"}",
                        "SELLER"));
    }
}
