package com.aibi.sales_service.dto;

import com.aibi.sales_service.entity.Sale;
import com.aibi.sales_service.model.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Schema(description = "Représentation publique d'une vente")
public record SaleResponse(
        @Schema(description = "Identifiant unique de la vente", example = "1")
        Long id,

        @Schema(description = "Identifiant du locataire propriétaire")
        UUID tenantId,

        @Schema(description = "Référence unique de la vente", example = "SAL-20261021-0001")
        String reference,

        @Schema(description = "Identifiant du client associé", example = "1")
        Long customerId,

        @Schema(description = "Statut actuel de la vente", example = "PENDING")
        SaleStatus status,

        @Schema(description = "Montant total recalculé par le serveur", example = "3999.98")
        BigDecimal totalAmount,

        @Schema(description = "Lignes d'articles composant la vente")
        List<SaleItemResponse> items,

        @Schema(description = "Date et heure de création", example = "2026-10-21T18:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Date et heure de dernière mise à jour", example = "2026-10-21T18:30:00")
        LocalDateTime updatedAt
) {
    public SaleResponse(
            Long id,
            String reference,
            Long customerId,
            SaleStatus status,
            BigDecimal totalAmount,
            List<SaleItemResponse> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this(id, null, reference, customerId, status, totalAmount, items, createdAt, updatedAt);
    }

    public static SaleResponse fromEntity(Sale sale) {
        if (sale == null) {
            return null;
        }
        List<SaleItemResponse> itemResponses = sale.getItems() != null
                ? sale.getItems().stream().map(SaleItemResponse::fromEntity).toList()
                : Collections.emptyList();

        return new SaleResponse(
                sale.getId(),
                sale.getTenantId(),
                sale.getReference(),
                sale.getCustomerId(),
                sale.getStatus(),
                sale.getTotalAmount(),
                itemResponses,
                sale.getCreatedAt(),
                sale.getUpdatedAt()
        );
    }
}
