package com.aibi.ai_service.provider.dto;

import java.util.Collections;
import java.util.List;

public record LlmResponse(
        String content,
        List<LlmToolCall> toolCalls,
        String providerName,
        String model
) {
    public LlmResponse {
        if (toolCalls == null) {
            toolCalls = Collections.emptyList();
        }
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public static LlmResponse ofText(String content, String providerName, String model) {
        return new LlmResponse(content, Collections.emptyList(), providerName, model);
    }

    public static LlmResponse ofToolCalls(List<LlmToolCall> toolCalls, String providerName, String model) {
        return new LlmResponse("", toolCalls, providerName, model);
    }
}
