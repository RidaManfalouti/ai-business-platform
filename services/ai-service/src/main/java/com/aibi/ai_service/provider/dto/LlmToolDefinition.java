package com.aibi.ai_service.provider.dto;

import java.util.Map;

public record LlmToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters
) {}
