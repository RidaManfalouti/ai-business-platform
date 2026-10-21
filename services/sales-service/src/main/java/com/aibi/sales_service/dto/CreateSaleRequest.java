package com.aibi.sales_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Requête de création d'une vente")
public record CreateSaleRequest(
        @Schema(description = "Identifiant du client associé", example = "1")
        @NotNull(message = "L'identifiant du client est obligatoire.")
        Long customerId,

        @Schema(description = "Référence unique optionnelle (ex: clé d'idempotence client)", example = "CMD-20261021-001")
        @Size(max = 64, message = "La référence ne peut excéder 64 caractères.")
        String reference,

        @Schema(description = "Liste des lignes de vente")
        @NotNull(message = "La liste des lignes de vente est obligatoire.")
        @NotEmpty(message = "La vente doit comporter au moins une ligne de produit.")
        @Valid
        List<SaleItemRequest> items
) {}
