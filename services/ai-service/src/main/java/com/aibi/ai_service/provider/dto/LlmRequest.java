package com.aibi.ai_service.provider.dto;

import java.util.Collections;
import java.util.List;

public record LlmRequest(
        String systemPrompt,
        List<LlmMessage> messages,
        List<LlmToolDefinition> tools,
        Double temperature,
        Integer maxTokens
) {
    public LlmRequest {
        if (messages == null) {
            messages = Collections.emptyList();
        }
        if (tools == null) {
            tools = Collections.emptyList();
        }
    }

    public static LlmRequest of(String systemPrompt, List<LlmMessage> messages, List<LlmToolDefinition> tools) {
        return new LlmRequest(systemPrompt, messages, tools, 0.2, 1024);
    }
}
