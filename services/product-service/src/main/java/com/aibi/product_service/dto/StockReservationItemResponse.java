package com.aibi.product_service.dto;

import com.aibi.product_service.entity.StockReservationItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Détail d'une ligne de réservation de stock")
public record StockReservationItemResponse(
        @Schema(description = "Identifiant du produit", example = "1")
        Long productId,

        @Schema(description = "Quantité réservée", example = "2")
        Integer quantity
) {
    public static StockReservationItemResponse fromEntity(StockReservationItem item) {
        if (item == null) {
            return null;
        }
        return new StockReservationItemResponse(
                item.getProductId(),
                item.getQuantity()
        );
    }
}
