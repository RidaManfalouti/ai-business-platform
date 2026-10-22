package com.aibi.sales_service.controller;

import com.aibi.sales_service.dto.CreateSaleRequest;
import com.aibi.sales_service.dto.SaleResponse;
import com.aibi.sales_service.dto.UpdateSaleStatusRequest;
import com.aibi.sales_service.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@Tag(name = "Sales", description = "Endpoints de gestion des ventes et des commandes clients")
public class SaleController {

    private final SalesService salesService;

    public SaleController(SalesService salesService) {
        this.salesService = salesService;
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle vente", description = "Enregistre une nouvelle commande client avec recalcul des montants côté serveur et réservation atomique de stock")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vente créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides (ex: quantité nulle ou négative, corps vide)"),
            @ApiResponse(responseCode = "404", description = "Client ou produit distant introuvable"),
            @ApiResponse(responseCode = "409", description = "Conflit métier (ex: stock insuffisant, référence en doublon)"),
            @ApiResponse(responseCode = "502", description = "Erreur de réponse ou indisponibilité d'un service distant"),
            @ApiResponse(responseCode = "503", description = "Service distant injoignable ou timeout")
    })
    public ResponseEntity<SaleResponse> createSale(
            @Parameter(description = "Clé d'idempotence optionnelle (équivalente à la référence)", example = "CMD-20261022-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateSaleRequest request) {

        CreateSaleRequest effectiveRequest = request;
        if ((request.reference() == null || request.reference().isBlank())
                && idempotencyKey != null && !idempotencyKey.isBlank()) {
            effectiveRequest = new CreateSaleRequest(request.customerId(), idempotencyKey.trim(), request.items());
        }

        SaleResponse created = salesService.createSale(effectiveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une vente par son identifiant", description = "Retourne la vente complète ainsi que le détail de toutes ses lignes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vente trouvée"),
            @ApiResponse(responseCode = "404", description = "Vente introuvable")
    })
    public ResponseEntity<SaleResponse> getSaleById(
            @Parameter(description = "Identifiant numérique de la vente", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(salesService.getSaleById(id));
    }

    @GetMapping
    @Operation(summary = "Lister les ventes avec pagination et tri", description = "Récupère les ventes enregistrées selon les critères de pagination et tri")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des ventes")
    })
    public ResponseEntity<Page<SaleResponse>> getAllSales(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(salesService.getAllSales(pageable));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Lister les ventes associées à un client", description = "Retourne l'historique des ventes d'un client spécifique")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ventes du client récupérées avec succès")
    })
    public ResponseEntity<Page<SaleResponse>> getSalesByCustomerId(
            @Parameter(description = "Identifiant unique du client", example = "1")
            @PathVariable Long customerId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(salesService.getSalesByCustomerId(customerId, pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Modifier le statut d'une vente", description = "Applique une transition de statut autorisée (ex: PENDING -> CONFIRMED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut modifié avec succès"),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou statut non reconnu"),
            @ApiResponse(responseCode = "404", description = "Vente introuvable"),
            @ApiResponse(responseCode = "409", description = "Transition de statut interdite par les règles métier")
    })
    public ResponseEntity<SaleResponse> updateSaleStatus(
            @Parameter(description = "Identifiant de la vente à modifier", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateSaleStatusRequest request) {
        return ResponseEntity.ok(salesService.updateSaleStatus(id, request));
    }
}
