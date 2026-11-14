package com.aibi.ai_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDetailResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages
) {}
