package com.aibi.product_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Requête de réservation de stock multi-articles")
public record StockReservationRequest(
        @Schema(description = "Identifiant unique de la réservation (clé d'idempotence, ex: référence de vente)", example = "SAL-20261022-ABC12345")
        @NotBlank(message = "L'identifiant de réservation est obligatoire.")
        @Size(max = 100, message = "L'identifiant de réservation ne peut excéder 100 caractères.")
        String reservationId,

        @Schema(description = "Liste des articles et quantités à réserver")
        @NotNull(message = "La liste des articles est obligatoire.")
        @NotEmpty(message = "La réservation doit comporter au moins un article.")
        @Valid
        List<StockReservationItemRequest> items
) {}
