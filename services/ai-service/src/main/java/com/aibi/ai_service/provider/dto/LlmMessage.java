package com.aibi.ai_service.provider.dto;

import java.util.Collections;
import java.util.List;

public record LlmMessage(
        String role, // "system", "user", "assistant", "tool"
        String content,
        String toolCallId,
        List<LlmToolCall> toolCalls
) {
    public LlmMessage(String role, String content) {
        this(role, content, null, Collections.emptyList());
    }

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content);
    }

    public static LlmMessage assistantWithTools(String content, List<LlmToolCall> toolCalls) {
        return new LlmMessage("assistant", content, null, toolCalls != null ? toolCalls : Collections.emptyList());
    }

    public static LlmMessage tool(String toolCallId, String content) {
        return new LlmMessage("tool", content, toolCallId, Collections.emptyList());
    }
}
