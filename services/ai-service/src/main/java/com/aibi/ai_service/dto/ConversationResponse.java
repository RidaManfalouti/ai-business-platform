package com.aibi.ai_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {}
