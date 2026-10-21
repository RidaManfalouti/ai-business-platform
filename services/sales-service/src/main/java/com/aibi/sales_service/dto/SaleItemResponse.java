package com.aibi.sales_service.dto;

import com.aibi.sales_service.entity.SaleItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Détail d'une ligne de vente")
public record SaleItemResponse(
        @Schema(description = "Identifiant de la ligne", example = "10")
        Long id,

        @Schema(description = "Identifiant du produit", example = "1")
        Long productId,

        @Schema(description = "Nom du produit capturé au moment de la vente", example = "MacBook Pro M3")
        String productNameSnapshot,

        @Schema(description = "Quantité achetée", example = "2")
        Integer quantity,

        @Schema(description = "Prix unitaire figé lors de la vente", example = "1999.99")
        BigDecimal unitPrice,

        @Schema(description = "Montant total de la ligne (quantité * prix unitaire)", example = "3999.98")
        BigDecimal lineTotal
) {
    public static SaleItemResponse fromEntity(SaleItem item) {
        if (item == null) {
            return null;
        }
        return new SaleItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
