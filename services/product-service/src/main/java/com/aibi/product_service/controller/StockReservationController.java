package com.aibi.product_service.controller;

import com.aibi.product_service.dto.StockReleaseResponse;
import com.aibi.product_service.dto.StockReservationRequest;
import com.aibi.product_service.dto.StockReservationResponse;
import com.aibi.product_service.service.StockReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/reservations")
@Tag(name = "Stock Reservation", description = "Endpoints de réservation et compensation de stock de produits")
public class StockReservationController {

    private final StockReservationService reservationService;

    public StockReservationController(StockReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Réserver du stock de façon atomique et idempotente",
            description = "Décrémente le stock en base de façon atomique conditionnelle. Si le stock est insuffisant, la requête est rejetée sans effet de bord.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock réservé avec succès"),
            @ApiResponse(responseCode = "200", description = "Réservation existante retournée (idempotence)"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides"),
            @ApiResponse(responseCode = "404", description = "Un ou plusieurs produits demandés sont introuvables"),
            @ApiResponse(responseCode = "409", description = "Stock insuffisant pour satisfaire la réservation")
    })
    public ResponseEntity<StockReservationResponse> reserveStock(
            @Valid @RequestBody StockReservationRequest request) {
        StockReservationResponse response = reservationService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{reservationId}/release")
    @Operation(summary = "Libérer une réservation de stock (compensation Saga)",
            description = "Réincrémente le stock des produits et marque la réservation comme libérée de manière idempotente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation libérée avec succès (ou déjà libérée)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<StockReleaseResponse> releaseStock(
            @Parameter(description = "Identifiant unique de la réservation à libérer", example = "SAL-20261022-ABC12345")
            @PathVariable String reservationId) {
        StockReleaseResponse response = reservationService.releaseStock(reservationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Consulter le statut d'une réservation de stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    public ResponseEntity<StockReservationResponse> getReservation(
            @Parameter(description = "Identifiant unique de la réservation", example = "SAL-20261022-ABC12345")
            @PathVariable String reservationId) {
        StockReservationResponse response = reservationService.getReservation(reservationId);
        return ResponseEntity.ok(response);
    }
}
