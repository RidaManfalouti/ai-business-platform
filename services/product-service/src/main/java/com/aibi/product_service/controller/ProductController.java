package com.aibi.product_service.controller;

import com.aibi.product_service.dto.ProductRequest;
import com.aibi.product_service.dto.ProductResponse;
import com.aibi.product_service.service.ProductService;
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
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Endpoints de gestion du catalogue de produits")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Créer un nouveau produit", description = "Enregistre un nouveau produit dans le catalogue")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produit créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données de validation invalides"),
        @ApiResponse(responseCode = "409", description = "Un produit avec ce nom existe déjà")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un produit par ID", description = "Retourne les détails d'un produit spécifique")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produit trouvé"),
        @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Identifiant du produit") @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    @Operation(summary = "Lister les produits avec pagination et tri", description = "Récupère les produits de manière paginée et triée")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée des produits")
    })
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @Parameter(description = "Filtre optionnel de recherche par nom")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(productService.searchProductsByName(search, pageable));
        }
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des produits par nom", description = "Recherche les produits dont le nom contient le terme spécifié")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Résultats paginés de la recherche")
    })
    public ResponseEntity<Page<ProductResponse>> searchProductsByName(
            @Parameter(description = "Terme de recherche dans le nom du produit", required = true)
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(productService.searchProductsByName(name, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un produit existant", description = "Met à jour l'ensemble des informations d'un produit")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produit modifié avec succès"),
        @ApiResponse(responseCode = "400", description = "Données de validation invalides"),
        @ApiResponse(responseCode = "404", description = "Produit non trouvé"),
        @ApiResponse(responseCode = "409", description = "Le nouveau nom est déjà utilisé par un autre produit")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Identifiant du produit à modifier") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un produit", description = "Supprime définitivement un produit du catalogue")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Produit supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Produit non trouvé")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Identifiant du produit à supprimer") @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
