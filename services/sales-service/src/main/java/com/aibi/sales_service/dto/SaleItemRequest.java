package com.aibi.sales_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Ligne de commande pour la création d'une vente")
public record SaleItemRequest(
        @Schema(description = "Identifiant du produit", example = "1")
        @NotNull(message = "L'identifiant du produit est obligatoire.")
        Long productId,

        @Schema(description = "Quantité commandée", example = "2")
        @NotNull(message = "La quantité est obligatoire.")
        @Min(value = 1, message = "La quantité doit être strictement positive (minimum 1).")
        Integer quantity
) {}
