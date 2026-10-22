package com.aibi.product_service.dto;

import com.aibi.product_service.entity.StockReservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Schema(description = "Réponse suite à une réservation de stock")
public record StockReservationResponse(
        @Schema(description = "Identifiant unique de la réservation", example = "SAL-20261022-ABC12345")
        String reservationId,

        @Schema(description = "Statut actuel de la réservation (ex: RESERVED, RELEASED)", example = "RESERVED")
        String status,

        @Schema(description = "Lignes d'articles réservés")
        List<StockReservationItemResponse> items,

        @Schema(description = "Date et heure de création de la réservation")
        LocalDateTime createdAt
) {
    public static StockReservationResponse fromEntity(StockReservation reservation) {
        if (reservation == null) {
            return null;
        }
        List<StockReservationItemResponse> itemResponses = reservation.getItems() != null
                ? reservation.getItems().stream().map(StockReservationItemResponse::fromEntity).toList()
                : Collections.emptyList();

        return new StockReservationResponse(
                reservation.getReservationId(),
                reservation.getStatus().name(),
                itemResponses,
                reservation.getCreatedAt()
        );
    }
}
