package com.aibi.analytics_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Représentation analytique non-PII d'un client à fort impact")
public record TopCustomerItem(
        @Schema(description = "Identifiant unique du client", example = "42")
        Long customerId,

        @Schema(description = "Nom commercial ou raison sociale (sans coordonnées directes)", example = "Entreprise ABC")
        String customerName,

        @Schema(description = "Nombre de commandes menées à terme (statut COMPLETED)", example = "12")
        long completedSalesCount,

        @Schema(description = "Montant total cumulé des achats terminés", example = "12450.00")
        BigDecimal totalSpent,

        @Schema(description = "Devise des montants", example = "EUR")
        String currency
) {}
