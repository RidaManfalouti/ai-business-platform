package com.aibi.analytics_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Classement analytique des meilleurs clients par volume d'achats terminés")
public record TopCustomersResponse(
        @Schema(description = "Liste des clients ordonnée par volume d'achat décroissant")
        List<TopCustomerItem> topCustomers,

        @Schema(description = "Nombre de clients retournés dans cette analyse")
        int totalCount
) {}
