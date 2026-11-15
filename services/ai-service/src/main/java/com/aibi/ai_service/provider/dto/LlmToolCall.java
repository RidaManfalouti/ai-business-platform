package com.aibi.ai_service.provider.dto;

public record LlmToolCall(
        String id,
        String name,
        String argumentsJson,
        String thoughtSignature
) {
    public LlmToolCall(String id, String name, String argumentsJson) {
        this(id, name, argumentsJson, null);
    }
}
