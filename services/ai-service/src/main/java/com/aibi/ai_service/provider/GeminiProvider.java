package com.aibi.ai_service.provider;

import com.aibi.ai_service.exception.LlmServiceException;
import com.aibi.ai_service.provider.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Component("geminiProvider")
public class GeminiProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiProvider(LlmProperties properties, ObjectMapper objectMapper) {
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
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        String key = properties.getGemini().getApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public LlmResponse generateResponse(LlmRequest request) {
        if (!isAvailable()) {
            throw new LlmServiceException("Clé d'API Google Gemini non configurée (GEMINI_API_KEY).");
        }

        String model = properties.getGemini().getModel();
        String baseUrl = properties.getGemini().getBaseUrl();
        String apiKey = properties.getGemini().getApiKey();

        String url = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        Map<String, Object> payload = buildGeminiPayload(request);

        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                String responseBody = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                return parseGeminiResponse(responseBody, model);
            } catch (Exception e) {
                lastException = e;
                log.warn("Gemini API call attempt {}/{} failed: {}", attempt, maxAttempts, sanitizeMessage(e.getMessage()));
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

        log.error("Toutes les tentatives vers le fournisseur Gemini ont échoué.");
        throw new LlmServiceException("Erreur lors de la communication avec Google Gemini", lastException);
    }

    private Map<String, Object> buildGeminiPayload(LlmRequest request) {
        Map<String, Object> root = new HashMap<>();

        // System Instruction
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            root.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", request.systemPrompt()))
            ));
        }

        // Contents
        List<Map<String, Object>> contents = new ArrayList<>();
        for (LlmMessage msg : request.messages()) {
            String geminiRole = "user";
            if ("assistant".equalsIgnoreCase(msg.role())) {
                geminiRole = "model";
            }

            List<Map<String, Object>> parts = new ArrayList<>();
            if ("tool".equalsIgnoreCase(msg.role())) {
                geminiRole = "user";
                Map<String, Object> funcResp = new HashMap<>();
                funcResp.put("name", msg.toolCallId() != null ? msg.toolCallId() : "toolResult");
                funcResp.put("response", Map.of("content", msg.content()));
                parts.add(Map.of("functionResponse", funcResp));
            } else {
                if (msg.content() != null && !msg.content().isBlank()) {
                    parts.add(Map.of("text", msg.content()));
                }
                if (msg.toolCalls() != null) {
                    for (LlmToolCall call : msg.toolCalls()) {
                        Map<String, Object> funcCall = new HashMap<>();
                        funcCall.put("name", call.name());
                        try {
                            funcCall.put("args", objectMapper.readValue(call.argumentsJson(), Map.class));
                        } catch (Exception ignored) {
                            funcCall.put("args", Collections.emptyMap());
                        }
                        parts.add(Map.of("functionCall", funcCall));
                    }
                }
            }

            if (!parts.isEmpty()) {
                contents.add(Map.of(
                        "role", geminiRole,
                        "parts", parts
                ));
            }
        }
        root.put("contents", contents);

        // Tools
        if (request.tools() != null && !request.tools().isEmpty()) {
            List<Map<String, Object>> declarations = new ArrayList<>();
            for (LlmToolDefinition tool : request.tools()) {
                Map<String, Object> decl = new HashMap<>();
                decl.put("name", tool.name());
                decl.put("description", tool.description());
                if (tool.parameters() != null) {
                    decl.put("parameters", tool.parameters());
                }
                declarations.add(decl);
            }
            root.put("tools", List.of(Map.of("functionDeclarations", declarations)));
        }

        return root;
    }

    private LlmResponse parseGeminiResponse(String responseBody, String model) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidate = root.path("candidates").path(0);
            JsonNode parts = candidate.path("content").path("parts");

            StringBuilder textBuilder = new StringBuilder();
            List<LlmToolCall> toolCalls = new ArrayList<>();

            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        textBuilder.append(part.get("text").asText());
                    }
                    if (part.has("functionCall")) {
                        JsonNode funcCall = part.get("functionCall");
                        String name = funcCall.path("name").asText();
                        JsonNode argsNode = funcCall.path("args");
                        String argsJson = argsNode.isMissingNode() ? "{}" : objectMapper.writeValueAsString(argsNode);
                        toolCalls.add(new LlmToolCall("call_" + UUID.randomUUID().toString().substring(0, 8), name, argsJson));
                    }
                }
            }

            return new LlmResponse(textBuilder.toString().trim(), toolCalls, getProviderName(), model);
        } catch (Exception e) {
            throw new LlmServiceException("Erreur lors de l'analyse de la réponse Gemini", e);
        }
    }

    private String sanitizeMessage(String msg) {
        if (msg == null) return "";
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            return msg.replace(apiKey, "[REDACTED_API_KEY]");
        }
        return msg;
    }
}
