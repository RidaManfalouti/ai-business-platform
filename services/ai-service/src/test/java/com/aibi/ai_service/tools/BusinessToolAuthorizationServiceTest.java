package com.aibi.ai_service.tools;

import com.aibi.ai_service.provider.dto.LlmToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BusinessToolAuthorizationServiceTest {

    private BusinessToolAuthorizationService authService;
    private List<LlmToolDefinition> sampleTools;

    @BeforeEach
    void setUp() {
        authService = new BusinessToolAuthorizationService();
        sampleTools = List.of(
                new LlmToolDefinition("getSalesSummary", "Sales summary", Map.of()),
                new LlmToolDefinition("getSalesByPeriod", "Sales by period", Map.of()),
                new LlmToolDefinition("compareRevenuePeriods", "Compare revenue", Map.of()),
                new LlmToolDefinition("getTopCustomers", "Top customers", Map.of()),
                new LlmToolDefinition("getTopSellingProducts", "Top products", Map.of()),
                new LlmToolDefinition("getLowStockProducts", "Low stock", Map.of())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "ADMIN", "MANAGER", "ROLE_OWNER", "ROLE_ADMIN", "ROLE_MANAGER"})
    @DisplayName("OWNER, ADMIN, MANAGER ont accès à TOUS les outils métier")
    void testHighPrivilegeRoles_HaveAccessToAllTools(String role) {
        assertTrue(authService.isToolAuthorized("getSalesSummary", role));
        assertTrue(authService.isToolAuthorized("getSalesByPeriod", role));
        assertTrue(authService.isToolAuthorized("compareRevenuePeriods", role));
        assertTrue(authService.isToolAuthorized("getTopCustomers", role));
        assertTrue(authService.isToolAuthorized("getTopSellingProducts", role));
        assertTrue(authService.isToolAuthorized("getLowStockProducts", role));

        List<LlmToolDefinition> filtered = authService.filterAuthorizedTools(sampleTools, role);
        assertEquals(6, filtered.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELLER", "VIEWER", "ROLE_SELLER", "ROLE_VIEWER"})
    @DisplayName("SELLER et VIEWER ont accès UNIQUEMENT aux outils catalogue et stocks (pas aux outils financiers)")
    void testStandardRoles_RestrictedFromFinancialTools(String role) {
        // Operational tools allowed
        assertTrue(authService.isToolAuthorized("getTopSellingProducts", role));
        assertTrue(authService.isToolAuthorized("getLowStockProducts", role));

        // Financial & customer analytics tools denied
        assertFalse(authService.isToolAuthorized("getSalesSummary", role));
        assertFalse(authService.isToolAuthorized("getSalesByPeriod", role));
        assertFalse(authService.isToolAuthorized("compareRevenuePeriods", role));
        assertFalse(authService.isToolAuthorized("getTopCustomers", role));

        List<LlmToolDefinition> filtered = authService.filterAuthorizedTools(sampleTools, role);
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(t -> t.name().equals("getTopSellingProducts")));
        assertTrue(filtered.stream().anyMatch(t -> t.name().equals("getLowStockProducts")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN_ROLE", "GUEST", "ANONYMOUS", "HACKER"})
    @DisplayName("Rôles inconnus sont rejetés par défaut (Fail-Closed)")
    void testUnknownRoles_DeniedByDefault(String role) {
        assertFalse(authService.isToolAuthorized("getSalesSummary", role));
        assertFalse(authService.isToolAuthorized("getTopSellingProducts", role));
        assertFalse(authService.isToolAuthorized("getLowStockProducts", role));

        List<LlmToolDefinition> filtered = authService.filterAuthorizedTools(sampleTools, role);
        assertTrue(filtered.isEmpty());
    }

    @Test
    @DisplayName("Rôle ou nom d'outil null/vide -> Rejet par défaut")
    void testNullOrBlank_Denied() {
        assertFalse(authService.isToolAuthorized(null, "OWNER"));
        assertFalse(authService.isToolAuthorized("", "OWNER"));
        assertFalse(authService.isToolAuthorized("getSalesSummary", null));
        assertFalse(authService.isToolAuthorized("getSalesSummary", "   "));

        assertTrue(authService.filterAuthorizedTools(sampleTools, null).isEmpty());
        assertTrue(authService.filterAuthorizedTools(null, "OWNER").isEmpty());
    }
}
