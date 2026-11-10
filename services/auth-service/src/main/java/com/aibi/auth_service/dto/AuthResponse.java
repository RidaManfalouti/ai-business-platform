package com.aibi.auth_service.dto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String email,
        String role,
        UUID tenantId,
        String tenantSlug,
        String tenantRole,
        List<TenantSummaryResponse> availableTenants
) {
    public AuthResponse(
            String token,
            String tokenType,
            Long userId,
            String username,
            String email,
            String role) {
        this(token, tokenType, userId, username, email, role, null, null, null, Collections.emptyList());
    }
}
