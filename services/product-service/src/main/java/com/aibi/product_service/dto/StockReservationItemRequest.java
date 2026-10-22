package com.aibi.product_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ligne d'article pour une réservation de stock")
public record StockReservationItemRequest(
        @Schema(description = "Identifiant unique du produit", example = "1")
        @NotNull(message = "L'identifiant du produit est obligatoire.")
        Long productId,

        @Schema(description = "Quantité à réserver", example = "2")
        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité à réserver doit être strictement positive (minimum 1).")
        Integer quantity
) {}
