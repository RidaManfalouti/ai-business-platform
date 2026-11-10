package com.aibi.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
    @NotBlank(message = "Le nom de l'entreprise est obligatoire.")
    @Size(min = 2, max = 100, message = "Le nom doit comporter entre 2 et 100 caractères.")
    String name,

    @Size(max = 100, message = "Le slug ne peut pas dépasser 100 caractères.")
    String slug
) {}
