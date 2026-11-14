package com.aibi.ai_service.provider;

import com.aibi.ai_service.exception.LlmServiceException;
import com.aibi.ai_service.provider.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Component("openAiProvider")
public class OpenAiProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiProvider(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.getTimeout().getConnectMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.getTimeout().getReadMs()));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isAvailable() {
        String key = properties.getOpenai().getApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public LlmResponse generateResponse(LlmRequest request) {
        if (!isAvailable()) {
            throw new LlmServiceException("Clé d'API OpenAI non configurée (OPENAI_API_KEY).");
        }

        String model = properties.getOpenai().getModel();
        String baseUrl = properties.getOpenai().getBaseUrl();
        String apiKey = properties.getOpenai().getApiKey();
        String url = baseUrl + "/v1/chat/completions";

        Map<String, Object> payload = buildOpenAiPayload(request, model);

        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                String responseBody = restClient.post()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                return parseOpenAiResponse(responseBody, model);
            } catch (Exception e) {
                lastException = e;
                log.warn("OpenAI API call attempt {}/{} failed: {}", attempt, maxAttempts, sanitizeMessage(e.getMessage()));
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Toutes les tentatives vers le fournisseur OpenAI ont échoué.");
        throw new LlmServiceException("Erreur lors de la communication avec OpenAI", lastException);
    }

    private Map<String, Object> buildOpenAiPayload(LlmRequest request, String model) {
        Map<String, Object> root = new HashMap<>();
        root.put("model", model);
        if (request.temperature() != null) {
            root.put("temperature", request.temperature());
        }
        if (request.maxTokens() != null) {
            root.put("max_tokens", request.maxTokens());
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }

        for (LlmMessage msg : request.messages()) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("role", msg.role());
            msgMap.put("content", msg.content() != null ? msg.content() : "");

            if ("tool".equalsIgnoreCase(msg.role()) && msg.toolCallId() != null) {
                msgMap.put("tool_call_id", msg.toolCallId());
            }

            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                for (LlmToolCall tc : msg.toolCalls()) {
                    toolCalls.add(Map.of(
                            "id", tc.id(),
                            "type", "function",
                            "function", Map.of(
                                    "name", tc.name(),
                                    "arguments", tc.argumentsJson()
                            )
                    ));
                }
                msgMap.put("tool_calls", toolCalls);
            }
            messages.add(msgMap);
        }
        root.put("messages", messages);

        if (request.tools() != null && !request.tools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (LlmToolDefinition tool : request.tools()) {
                tools.add(Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", tool.parameters() != null ? tool.parameters() : Map.of("type", "object")
                        )
                ));
            }
            root.put("tools", tools);
        }

        return root;
    }

    private LlmResponse parseOpenAiResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choice = root.path("choices").path(0);
            JsonNode message = choice.path("message");

            String content = message.path("content").asText("");
            List<LlmToolCall> toolCalls = new ArrayList<>();

            JsonNode toolCallsNode = message.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode tcNode : toolCallsNode) {
                    String id = tcNode.path("id").asText();
                    String name = tcNode.path("function").path("name").asText();
                    String args = tcNode.path("function").path("arguments").asText("{}");
                    toolCalls.add(new LlmToolCall(id, name, args));
                }
            }

            return new LlmResponse(content, toolCalls, getProviderName(), model);
        } catch (Exception e) {
            throw new LlmServiceException("Erreur lors de l'analyse de la réponse OpenAI", e);
        }
    }

    private String sanitizeMessage(String msg) {
        if (msg == null) return "";
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            return msg.replace(apiKey, "[REDACTED_API_KEY]");
        }
        return msg;
    }
}
