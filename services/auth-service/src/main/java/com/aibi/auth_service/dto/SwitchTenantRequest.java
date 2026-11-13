package com.aibi.auth_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SwitchTenantRequest(
    @NotNull(message = "L'identifiant du locataire est obligatoire.")
    UUID tenantId
) {}
