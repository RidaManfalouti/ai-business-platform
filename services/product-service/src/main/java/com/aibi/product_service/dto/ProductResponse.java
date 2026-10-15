package com.aibi.product_service.dto;

import com.aibi.product_service.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Représentation publique d'un produit")
public record ProductResponse(
    @Schema(description = "Identifiant unique du produit", example = "1")
    Long id,

    @Schema(description = "Nom du produit", example = "MacBook Pro M3")
    String name,

    @Schema(description = "Description du produit", example = "Ordinateur portable haute performance Apple")
    String description,

    @Schema(description = "Prix unitaire", example = "1999.99")
    BigDecimal price,

    @Schema(description = "Quantité en stock", example = "25")
    Integer stockQuantity,

    @Schema(description = "Catégorie", example = "Informatique")
    String category,

    @Schema(description = "Date et heure de création", example = "2026-10-15T18:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Date et heure de dernière modification", example = "2026-10-15T18:30:00")
    LocalDateTime updatedAt
) {
    public static ProductResponse fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getCategory(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
