package com.aibi.ai_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
        UUID messageId,
        String role,
        String response,
        Instant createdAt
) {}
