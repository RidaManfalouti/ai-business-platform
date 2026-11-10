package com.aibi.auth_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TenantResponse(
    UUID id,
    String name,
    String slug,
    String status,
    String role,
    LocalDateTime createdAt
) {}
