package com.aibi.ai_service.tools;

import com.aibi.ai_service.provider.dto.LlmToolDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BusinessToolRegistry {

    private final List<LlmToolDefinition> availableTools;

    public BusinessToolRegistry() {
        this.availableTools = initTools();
    }

    public List<LlmToolDefinition> getAvailableTools() {
        return availableTools;
    }

    private List<LlmToolDefinition> initTools() {
        List<LlmToolDefinition> tools = new ArrayList<>();

        // 1. getSalesSummary
        tools.add(new LlmToolDefinition(
                "getSalesSummary",
                "Retourne les indicateurs de performance des ventes (chiffre d'affaires total, nombre de ventes terminées, panier moyen, nombre de clients et produits) pour une période donnée.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "from", Map.of("type", "string", "description", "Date de début optionnelle au format YYYY-MM-DD"),
                                "to", Map.of("type", "string", "description", "Date de fin optionnelle au format YYYY-MM-DD"),
                                "timezone", Map.of("type", "string", "description", "Fuseau horaire optionnel, ex: UTC, Europe/Paris")
                        )
                )
        ));

        // 2. getSalesByPeriod
        tools.add(new LlmToolDefinition(
                "getSalesByPeriod",
                "Retourne l'évolution et les tendances quotidiennes de chiffre d'affaires et de volume de commandes sur une période.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "from", Map.of("type", "string", "description", "Date de début au format YYYY-MM-DD"),
                                "to", Map.of("type", "string", "description", "Date de fin au format YYYY-MM-DD"),
                                "timezone", Map.of("type", "string", "description", "Fuseau horaire, ex: Europe/Paris")
                        )
                )
        ));

        // 3. getTopSellingProducts
        tools.add(new LlmToolDefinition(
                "getTopSellingProducts",
                "Retourne le classement des produits les plus vendus calculé à partir des ventes terminées.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "from", Map.of("type", "string", "description", "Date de début optionnelle YYYY-MM-DD"),
                                "to", Map.of("type", "string", "description", "Date de fin optionnelle YYYY-MM-DD"),
                                "limit", Map.of("type", "integer", "description", "Nombre maximum de produits (défaut 5, max 20)")
                        )
                )
        ));

        // 4. getLowStockProducts
        tools.add(new LlmToolDefinition(
                "getLowStockProducts",
                "Retourne les alertes de stock pour les produits dont la quantité disponible est inférieure ou égale au seuil critique.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "threshold", Map.of("type", "integer", "description", "Seuil de stock critique (défaut 10)"),
                                "limit", Map.of("type", "integer", "description", "Nombre maximum d'articles retournés (défaut 10, max 50)")
                        )
                )
        ));

        // 5. getTopCustomers
        tools.add(new LlmToolDefinition(
                "getTopCustomers",
                "Retourne la liste des clients de l'entreprise avec leurs coordonnées.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "limit", Map.of("type", "integer", "description", "Nombre maximum de clients (défaut 10, max 25)")
                        )
                )
        ));

        // 6. compareRevenuePeriods
        tools.add(new LlmToolDefinition(
                "compareRevenuePeriods",
                "Compare les performances financières et de ventes entre deux périodes distinctes (ex: ce mois vs mois dernier).",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "period1From", Map.of("type", "string", "description", "Période 1 - Début YYYY-MM-DD"),
                                "period1To", Map.of("type", "string", "description", "Période 1 - Fin YYYY-MM-DD"),
                                "period2From", Map.of("type", "string", "description", "Période 2 - Début YYYY-MM-DD"),
                                "period2To", Map.of("type", "string", "description", "Période 2 - Fin YYYY-MM-DD")
                        ),
                        "required", List.of("period1From", "period1To", "period2From", "period2To")
                )
        ));

        return List.copyOf(tools);
    }
}
