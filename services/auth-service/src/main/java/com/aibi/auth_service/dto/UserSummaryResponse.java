package com.aibi.auth_service.dto;

import java.time.LocalDateTime;

public record UserSummaryResponse(
    Long id,
    String username,
    String email,
    String role,
    LocalDateTime createdAt
) {}
