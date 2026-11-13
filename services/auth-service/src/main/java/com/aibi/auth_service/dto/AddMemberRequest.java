package com.aibi.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
    @NotBlank(message = "L'email est obligatoire.")
    @Email(message = "Format d'email invalide.")
    String email,

    @NotBlank(message = "Le rôle est obligatoire.")
    String role
) {}
