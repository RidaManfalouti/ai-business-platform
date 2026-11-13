package com.aibi.auth_service.dto;

import java.util.UUID;

public record TenantSummaryResponse(
    UUID id,
    String name,
    String slug,
    String status,
    String role
) {}
