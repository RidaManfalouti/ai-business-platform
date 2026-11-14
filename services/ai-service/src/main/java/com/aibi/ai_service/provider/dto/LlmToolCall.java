package com.aibi.ai_service.provider.dto;

import java.util.List;

public record LlmToolCall(
        String id,
        String name,
        String argumentsJson
) {}
