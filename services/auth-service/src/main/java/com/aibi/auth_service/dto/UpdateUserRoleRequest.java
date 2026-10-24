package com.aibi.auth_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(
    @NotBlank(message = "Le rôle est obligatoire.")
    String role
) {}
