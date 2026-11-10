package com.aibi.auth_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MembershipResponse(
    UUID id,
    UUID tenantId,
    Long userId,
    String username,
    String email,
    String role,
    String status,
    LocalDateTime createdAt
) {}
