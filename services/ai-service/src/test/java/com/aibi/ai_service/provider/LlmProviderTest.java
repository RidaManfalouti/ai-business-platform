package com.aibi.ai_service.provider;

import com.aibi.ai_service.provider.dto.LlmMessage;
import com.aibi.ai_service.provider.dto.LlmRequest;
import com.aibi.ai_service.provider.dto.LlmResponse;
import com.aibi.ai_service.provider.dto.LlmToolDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LlmProviderTest {

    @Test
    @DisplayName("MockLlmProvider génère un appel d'outil getSalesSummary sur question de ventes")
    void testMockLlmProvider_TriggersTool() {
        MockLlmProvider provider = new MockLlmProvider();

        LlmToolDefinition salesTool = new LlmToolDefinition("getSalesSummary", "Ventes", Map.of());
        LlmRequest request = LlmRequest.of(
                "System prompt",
                List.of(LlmMessage.user("Quel est mon chiffre d'affaires et mes ventes ce mois ?")),
                List.of(salesTool)
        );

        LlmResponse response = provider.generateResponse(request);

        assertTrue(response.hasToolCalls());
        assertEquals("getSalesSummary", response.toolCalls().get(0).name());
    }

    @Test
    @DisplayName("MockLlmProvider synthétise le résultat d'un outil")
    void testMockLlmProvider_SummarizesToolResult() {
        MockLlmProvider provider = new MockLlmProvider();

        LlmRequest request = LlmRequest.of(
                "System prompt",
                List.of(
                        LlmMessage.user("Chiffre d'affaires ?"),
                        LlmMessage.tool("call_123", "{\"totalRevenue\":25000}")
                ),
                List.of()
        );

        LlmResponse response = provider.generateResponse(request);

        assertFalse(response.hasToolCalls());
        assertTrue(response.content().contains("25000"));
    }

    @Test
    @DisplayName("LlmProviderFactory bascule sur MockLlmProvider si clé Gemini absente")
    void testFactory_FallsBackToMockWhenKeyMissing() {
        LlmProperties properties = new LlmProperties();
        properties.setProvider("gemini");
        properties.getGemini().setApiKey(""); // Missing key

        MockLlmProvider mock = new MockLlmProvider();
        LlmProviderFactory factory = new LlmProviderFactory(properties, List.of(mock), mock);

        LlmProvider resolved = factory.getProvider();
        assertEquals("mock", resolved.getProviderName());
    }
}
