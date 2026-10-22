package com.aibi.product_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse suite à la libération d'une réservation de stock")
public record StockReleaseResponse(
        @Schema(description = "Identifiant de la réservation libérée", example = "SAL-20261022-ABC12345")
        String reservationId,

        @Schema(description = "Nouveau statut de la réservation", example = "RELEASED")
        String status,

        @Schema(description = "Message informatif", example = "Stock libéré avec succès.")
        String message
) {}
