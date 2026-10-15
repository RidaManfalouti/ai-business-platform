package com.aibi.product_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Requête de création ou mise à jour d'un produit")
public record ProductRequest(
    @Schema(description = "Nom unique du produit", example = "MacBook Pro M3")
    @NotBlank(message = "Le nom du produit est obligatoire.")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
    String name,

    @Schema(description = "Description détaillée du produit", example = "Ordinateur portable haute performance Apple")
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères.")
    String description,

    @Schema(description = "Prix unitaire du produit (positif ou nul)", example = "1999.99")
    @NotNull(message = "Le prix est obligatoire.")
    @PositiveOrZero(message = "Le prix doit être supérieur ou égal à zéro.")
    BigDecimal price,

    @Schema(description = "Quantité d'articles en stock", example = "25")
    @NotNull(message = "La quantité en stock est obligatoire.")
    @Min(value = 0, message = "La quantité en stock ne peut pas être négative.")
    Integer stockQuantity,

    @Schema(description = "Catégorie du produit", example = "Informatique")
    @NotBlank(message = "La catégorie est obligatoire.")
    @Size(max = 50, message = "La catégorie ne doit pas dépasser 50 caractères.")
    String category
) {}
