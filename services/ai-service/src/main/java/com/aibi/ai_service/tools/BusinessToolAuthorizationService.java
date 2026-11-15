package com.aibi.ai_service.tools;

import com.aibi.ai_service.provider.dto.LlmToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service centralisé d'autorisation serveur pour les outils IA (Business Tools).
 * Garantit qu'aucun outil d'analyse financière ou de données sensibles n'est exposé
 * ou exécuté par un utilisateur sans les privilèges adéquats.
 *
 * Politique RBAC stricte :
 * - OWNER, ADMIN, MANAGER : Accès complet à l'ensemble des outils opérationnels et financiers.
 * - SELLER : Accès restreint aux opérations du catalogue (getTopSellingProducts, getLowStockProducts).
 *   Refus systématique pour les métriques de chiffre d'affaires, comparaisons financières et clients.
 * - VIEWER : Consultation restreinte aux produits (getTopSellingProducts, getLowStockProducts).
 *   Refus systématique des outils financiers et de données clients.
 * - Rôles inconnus ou absents : Refus par défaut (Fail-Closed).
 */
@Service
public class BusinessToolAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(BusinessToolAuthorizationService.class);

    private static final Set<String> HIGH_PRIVILEGE_ROLES = Set.of("OWNER", "ADMIN", "MANAGER");
    private static final Set<String> STANDARD_ROLES = Set.of("SELLER", "VIEWER");

    // Outils opérationnels ouverts à tous les membres authentifiés du locataire
    private static final Set<String> OPERATIONAL_TOOLS = Set.of(
            "getTopSellingProducts",
            "getLowStockProducts"
    );

    // Outils financiers et de données stratégiques réservés aux gestionnaires et administrateurs
    private static final Set<String> RESTRICTED_FINANCIAL_TOOLS = Set.of(
            "getSalesSummary",
            "getSalesByPeriod",
            "compareRevenuePeriods",
            "getTopCustomers"
    );

    /**
     * Vérifie si un rôle donné est autorisé à invoquer un outil métier.
     *
     * @param toolName   Nom de l'outil métier
     * @param tenantRole Rôle du locataire extrait de manière vérifiée du JWT
     * @return true si l'accès est autorisé, false sinon (Fail-Closed)
     */
    public boolean isToolAuthorized(String toolName, String tenantRole) {
        if (toolName == null || toolName.isBlank() || tenantRole == null || tenantRole.isBlank()) {
            log.warn("Refus d'autorisation outil : nom d'outil [{}] ou rôle [{}] manquant", toolName, tenantRole);
            return false;
        }

        String normalizedRole = tenantRole.trim().toUpperCase();
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring(5);
        }

        if (HIGH_PRIVILEGE_ROLES.contains(normalizedRole)) {
            boolean authorized = OPERATIONAL_TOOLS.contains(toolName) || RESTRICTED_FINANCIAL_TOOLS.contains(toolName);
            if (!authorized) {
                log.warn("Outil inconnu [{}] demandé par le rôle [{}]", toolName, normalizedRole);
            }
            return authorized;
        }

        if (STANDARD_ROLES.contains(normalizedRole)) {
            boolean authorized = OPERATIONAL_TOOLS.contains(toolName);
            if (!authorized) {
                log.warn("Accès refusé : le rôle standard [{}] a tenté d'accéder à l'outil restreint [{}]", normalizedRole, toolName);
            }
            return authorized;
        }

        log.warn("Accès refusé : rôle locataire inconnu [{}] pour l'outil [{}]", normalizedRole, toolName);
        return false;
    }

    /**
     * Filtre la liste des définitions d'outils exposées au LLM pour ne présenter
     * que les outils autorisés pour le rôle courant.
     *
     * @param allTools   Ensemble des outils définis dans le registre
     * @param tenantRole Rôle du locataire extrait du JWT
     * @return Liste filtrée des outils autorisés
     */
    public List<LlmToolDefinition> filterAuthorizedTools(List<LlmToolDefinition> allTools, String tenantRole) {
        if (allTools == null || allTools.isEmpty() || tenantRole == null || tenantRole.isBlank()) {
            return Collections.emptyList();
        }
        return allTools.stream()
                .filter(tool -> isToolAuthorized(tool.name(), tenantRole))
                .toList();
    }
}
