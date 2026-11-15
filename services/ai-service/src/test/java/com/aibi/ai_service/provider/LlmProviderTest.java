package com.aibi.ai_service.provider;

import com.aibi.ai_service.provider.dto.LlmMessage;
import com.aibi.ai_service.provider.dto.LlmRequest;
import com.aibi.ai_service.provider.dto.LlmResponse;
import com.aibi.ai_service.provider.dto.LlmToolDefinition;
import com.aibi.ai_service.security.ProductionSecurityValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
    @DisplayName("CRIT-06 : En développement, LlmProviderFactory bascule sur MockLlmProvider si clé Gemini absente")
    void testFactory_FallsBackToMockInDevelopment() {
        StandardEnvironment env = new StandardEnvironment();
        LlmProperties properties = new LlmProperties();
        properties.setProvider("gemini");
        properties.getGemini().setApiKey(""); // Missing key

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, properties);
        MockLlmProvider mock = new MockLlmProvider();
        GeminiProvider gemini = new GeminiProvider(properties, new ObjectMapper());

        LlmProviderFactory factory = new LlmProviderFactory(properties, List.of(gemini, mock), mock, validator);

        LlmProvider resolved = factory.getProvider();
        assertEquals("mock", resolved.getProviderName());
    }

    @Test
    @DisplayName("CRIT-06 : En production, LlmProviderFactory rejette le provider mock")
    void testFactory_RejectsMockInProduction() {
        StandardEnvironment env = new StandardEnvironment();
        env.addActiveProfile("production");
        LlmProperties properties = new LlmProperties();
        properties.setProvider("mock");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, properties);
        MockLlmProvider mock = new MockLlmProvider();

        LlmProviderFactory factory = new LlmProviderFactory(properties, List.of(mock), mock, validator);

        IllegalStateException ex = assertThrows(IllegalStateException.class, factory::getProvider);
        assertTrue(ex.getMessage().contains("mock"));
    }

    @Test
    @DisplayName("CRIT-06 : En production, LlmProviderFactory refuse le repli silencieux vers le mock si Gemini est indisponible")
    void testFactory_NoSilentFallbackInProduction() {
        StandardEnvironment env = new StandardEnvironment();
        env.addActiveProfile("production");
        LlmProperties properties = new LlmProperties();
        properties.setProvider("gemini");
        properties.getGemini().setApiKey(""); // Missing key

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, properties);
        MockLlmProvider mock = new MockLlmProvider();
        GeminiProvider gemini = new GeminiProvider(properties, new ObjectMapper());

        LlmProviderFactory factory = new LlmProviderFactory(properties, List.of(gemini, mock), mock, validator);

        IllegalStateException ex = assertThrows(IllegalStateException.class, factory::getProvider);
        assertTrue(ex.getMessage().contains("gemini"));
    }

    @Test
    @DisplayName("CRIT-08 : GeminiProvider transmet la clé d'API via le header HTTP x-goog-api-key et JAMAIS dans l'URL")
    void testGeminiProvider_ApiKeyTransportSecurity() {
        String testApiKey = "AIzaSyTestSecretApiKeyForHeaderValidationOnly999";
        LlmProperties properties = new LlmProperties();
        properties.getGemini().setApiKey(testApiKey);
        properties.getGemini().setBaseUrl("https://generativelanguage.googleapis.com");
        properties.getGemini().setModel("gemini-2.5-flash");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        GeminiProvider provider = new GeminiProvider(properties, new ObjectMapper(), restClient);

        String mockResponseBody = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    {"text": "Bonjour, comment puis-je vous aider ?"}
                                ]
                            }
                        }
                    ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", testApiKey))
                .andExpect(request -> {
                    String uriString = request.getURI().toString();
                    assertFalse(uriString.contains("key="), "L'URL de la requête Gemini ne doit JAMAIS contenir le paramètre de clé d'API");
                    assertFalse(uriString.contains(testApiKey), "La clé d'API ne doit pas figurer dans l'URL");
                })
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        LlmRequest request = LlmRequest.of("System prompt", List.of(LlmMessage.user("Bonjour")), List.of());
        LlmResponse response = provider.generateResponse(request);

        assertNotNull(response);
        assertEquals("Bonjour, comment puis-je vous aider ?", response.content());
        mockServer.verify();
    }
}
