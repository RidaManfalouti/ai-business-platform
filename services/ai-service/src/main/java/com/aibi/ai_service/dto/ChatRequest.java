package com.aibi.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatRequest(
        UUID conversationId,
        @NotBlank(message = "Le message ne peut pas être vide")
        @Size(max = 4000, message = "Le message ne peut pas dépasser 4000 caractères")
        String message
) {}
