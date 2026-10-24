package com.aibi.customer_service.controller;

import com.aibi.customer_service.dto.CustomerRequest;
import com.aibi.customer_service.dto.CustomerResponse;
import com.aibi.customer_service.service.CustomerService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer", description = "Endpoints de gestion de la relation client")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Créer un nouveau client", description = "Enregistre un nouveau profil client")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Client créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données de validation invalides"),
        @ApiResponse(responseCode = "409", description = "Un client avec cet e-mail existe déjà")
    })
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Consulter un client par son identifiant", description = "Retourne les informations d'un client spécifique")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Client trouvé"),
        @ApiResponse(responseCode = "404", description = "Client introuvable")
    })
    public ResponseEntity<CustomerResponse> getCustomerById(
            @Parameter(description = "Identifiant unique du client") @PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lister les clients avec pagination et tri", description = "Récupère les clients de manière paginée et triée")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée des clients")
    })
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Rechercher des clients par nom ou par email", description = "Recherche multicritère insensible à la casse")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résultats paginés de la recherche")
    })
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @Parameter(description = "Recherche par prénom ou nom de famille")
            @RequestParam(required = false) String name,
            @Parameter(description = "Recherche par adresse email")
            @RequestParam(required = false) String email,
            @PageableDefault(size = 10, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        if (email != null && !email.isBlank()) {
            return ResponseEntity.ok(customerService.searchByEmail(email, pageable));
        }
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(customerService.searchByName(name, pageable));
        }
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Modifier les informations d'un client", description = "Met à jour l'ensemble des coordonnées d'un client")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Client modifié avec succès"),
        @ApiResponse(responseCode = "400", description = "Données de validation invalides"),
        @ApiResponse(responseCode = "404", description = "Client introuvable"),
        @ApiResponse(responseCode = "409", description = "L'adresse email est déjà utilisée par un autre client")
    })
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Identifiant du client à modifier") @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un client", description = "Supprime définitivement un client du système")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Client supprimé avec succès"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - Droits administrateur requis"),
        @ApiResponse(responseCode = "404", description = "Client introuvable")
    })
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Identifiant du client à supprimer") @PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
