package com.aibi.analytics_service.controller;

import com.aibi.analytics_service.dto.*;
import com.aibi.analytics_service.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Analytics", description = "Endpoints de métriques, KPIs et analyses décisionnelles en temps réel")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/kpi")
    @Operation(summary = "Indicateurs clés de performance (KPI)", description = "Retourne le chiffre d'affaires valide (ventes COMPLETED), le nombre de ventes terminées, le panier moyen et les volumes clients/produits")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPI calculés avec succès"),
            @ApiResponse(responseCode = "400", description = "Plage de dates invalide ou fuseau horaire erroné"),
            @ApiResponse(responseCode = "502", description = "Erreur de communication avec un service distant"),
            @ApiResponse(responseCode = "503", description = "Service distant indisponible")
    })
    public ResponseEntity<KpiSummaryResponse> getKpi(
            @Parameter(description = "Date de début ISO-8601 (ex: 2026-10-01)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Date de fin ISO-8601 (ex: 2026-10-27)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Identifiant de fuseau horaire (ex: Europe/Paris, UTC)")
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(analyticsService.getKpiSummary(from, to, timezone));
    }

    @GetMapping("/trends")
    @Operation(summary = "Tendances de chiffre d'affaires et volume", description = "Retourne l'évolution quotidienne du chiffre d'affaires et du nombre de commandes sur la période")
    public ResponseEntity<TrendsResponse> getTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(analyticsService.getTrends(from, to, timezone));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Classement des meilleures ventes", description = "Retourne les produits les plus vendus calculés à partir des lignes des ventes COMPLETED")
    public ResponseEntity<TopProductsResponse> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analyticsService.getTopProducts(from, to, timezone, limit));
    }

    @GetMapping("/status-distribution")
    @Operation(summary = "Répartition par statut de commande", description = "Retourne la distribution des commandes par état (PENDING, CONFIRMED, COMPLETED, CANCELLED)")
    public ResponseEntity<StatusDistributionResponse> getStatusDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        return ResponseEntity.ok(analyticsService.getStatusDistribution(from, to, timezone));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Alertes de stock faible", description = "Retourne les produits dont la quantité en inventaire est inférieure ou égale au seuil critique")
    public ResponseEntity<LowStockResponse> getLowStock(
            @RequestParam(required = false) Integer threshold,
            @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(analyticsService.getLowStock(threshold, limit));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Vue d'ensemble complète du tableau de bord", description = "Agrège l'ensemble des indicateurs (KPI, tendances, top produits, statuts, alertes stock) en un appel unique optimisé")
    public ResponseEntity<DashboardAnalyticsResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false, defaultValue = "5") Integer topLimit,
            @RequestParam(required = false) Integer lowStockThreshold) {
        return ResponseEntity.ok(analyticsService.getDashboard(from, to, timezone, topLimit, lowStockThreshold));
    }
}
