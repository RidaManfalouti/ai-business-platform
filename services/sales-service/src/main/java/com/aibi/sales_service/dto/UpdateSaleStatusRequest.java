package com.aibi.sales_service.dto;

import com.aibi.sales_service.model.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requête de mise à jour du statut d'une vente")
public record UpdateSaleStatusRequest(
        @Schema(description = "Nouveau statut souhaité", example = "CONFIRMED")
        @NotNull(message = "Le statut est obligatoire.")
        SaleStatus status
) {}
