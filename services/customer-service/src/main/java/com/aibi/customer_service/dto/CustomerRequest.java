package com.aibi.customer_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Requête de création ou mise à jour d'un client")
public record CustomerRequest(
    @Schema(description = "Prénom du client", example = "Jean")
    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 50, message = "Le prénom ne doit pas dépasser 50 caractères.")
    String firstName,

    @Schema(description = "Nom de famille du client", example = "Dupont")
    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 50, message = "Le nom ne doit pas dépasser 50 caractères.")
    String lastName,

    @Schema(description = "Adresse email unique", example = "jean.dupont@example.com")
    @NotBlank(message = "L'adresse email est obligatoire.")
    @Email(message = "Format d'adresse email invalide.")
    @Size(max = 100, message = "L'adresse email ne doit pas dépasser 100 caractères.")
    String email,

    @Schema(description = "Numéro de téléphone professionnel (facultatif)", example = "+33612345678")
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères.")
    String phone,

    @Schema(description = "Nom de l'entreprise ou organisation (facultatif)", example = "Acme Corp")
    @Size(max = 100, message = "Le nom de l'entreprise ne doit pas dépasser 100 caractères.")
    String company
) {}
