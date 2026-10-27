package com.aibi.sales_service.controller;

import com.aibi.sales_service.dto.DailyTrendResponse;
import com.aibi.sales_service.dto.SalesStatusCountResponse;
import com.aibi.sales_service.dto.SalesSummaryAggregateResponse;
import com.aibi.sales_service.dto.TopProductAggregateResponse;
import com.aibi.sales_service.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales/aggregates")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Sales Aggregates", description = "Endpoints d'agrégation SQL optimisés pour l'analytique")
public class SalesAggregationController {

    private final SalesService salesService;

    public SalesAggregationController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Résumé des ventes", description = "Retourne le chiffre d'affaires valide (ventes COMPLETED) et le volume total sur la période")
    public ResponseEntity<SalesSummaryAggregateResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(salesService.getSummaryAggregate(startDate, endDate));
    }

    @GetMapping("/status-distribution")
    @Operation(summary = "Distribution des statuts", description = "Retourne le décompte de chaque statut de commande sur la période")
    public ResponseEntity<List<SalesStatusCountResponse>> getStatusDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(salesService.getStatusDistribution(startDate, endDate));
    }

    @GetMapping("/trends")
    @Operation(summary = "Tendances quotidiennes", description = "Retourne le volume de ventes et CA réalisé par jour")
    public ResponseEntity<List<DailyTrendResponse>> getDailyTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(salesService.getDailyTrends(startDate, endDate));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Produits les plus vendus", description = "Retourne le classement des produits les plus vendus sur les ventes COMPLETED")
    public ResponseEntity<List<TopProductAggregateResponse>> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(salesService.getTopProducts(startDate, endDate, limit));
    }
}
