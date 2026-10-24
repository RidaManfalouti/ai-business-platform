package com.aibi.sales_service.client;

import com.aibi.sales_service.client.dto.*;
import com.aibi.sales_service.exception.InsufficientStockException;
import com.aibi.sales_service.exception.ProductNotFoundException;
import com.aibi.sales_service.exception.RemoteServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
public class ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClient.class);
    private final RestClient restClient;

    public ProductServiceClient(@Qualifier("productRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public StockReservationResponse reserveStock(StockReservationRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("La requête de réservation de stock ne peut être vide.");
        }

        try {
            return restClient.post()
                    .uri("/api/products/reservations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ProductNotFoundException("Un ou plusieurs produits demandés sont introuvables lors de la réservation.");
                    })
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        String bodyStr = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new InsufficientStockException("Stock insuffisant pour satisfaire la commande : " + bodyStr);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new RemoteServiceUnavailableException(
                                "Le service des produits est temporairement indisponible lors de la réservation de stock.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(StockReservationResponse.class);
        } catch (ProductNotFoundException | InsufficientStockException | RemoteServiceUnavailableException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai d'attente dépassé ou indisponibilité réseau lors de la réservation de stock pour {} : {}", request.reservationId(), ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Impossible de contacter le service des produits (délai d'attente dépassé ou indisponibilité réseau).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (RestClientResponseException ex) {
            log.error("Erreur HTTP reçue du Product Service ({}) lors de la réservation {} : {}", ex.getStatusCode(), request.reservationId(), ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Erreur de communication avec le service des produits lors de la réservation.",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de la réservation de stock pour {} : {}", request.reservationId(), ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Erreur inattendue lors de la réservation de stock.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public StockReleaseResponse releaseStock(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            log.warn("Tentative de libération de réservation avec un identifiant vide.");
            return null;
        }

        try {
            return restClient.post()
                    .uri("/api/products/reservations/{reservationId}/release", reservationId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        log.warn("Réservation {} introuvable pour libération dans Product Service.", reservationId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        log.error("Erreur serveur reçue de Product Service lors de la libération de la réservation {}", reservationId);
                        throw new RemoteServiceUnavailableException(
                                "Le service des produits est temporairement indisponible lors de la libération de stock.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(StockReleaseResponse.class);
        } catch (RemoteServiceUnavailableException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai d'attente dépassé lors de la libération de stock pour {} : {}", reservationId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Impossible de contacter le service des produits pour libérer le stock.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de la libération de stock pour {} : {}", reservationId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Erreur inattendue lors de la libération de stock.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public StockReservationResponse getReservation(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return null;
        }

        try {
            return restClient.get()
                    .uri("/api/products/reservations/{reservationId}", reservationId)
                    .retrieve()
                    .body(StockReservationResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                log.debug("Réservation {} non trouvée (404) dans Product Service.", reservationId);
                return null;
            }
            log.warn("Erreur HTTP lors de la consultation de la réservation {} : {}", reservationId, ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Erreur inattendue lors de la consultation de la réservation {} : {}", reservationId, ex.getMessage());
            return null;
        }
    }

    public ProductDto getProductById(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("L'identifiant du produit ne peut être nul.");
        }

        try {
            ProductDto product = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ProductNotFoundException("Produit introuvable avec l'identifiant : " + productId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new RemoteServiceUnavailableException(
                                "Le service des produits est temporairement indisponible.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(ProductDto.class);

            validateProduct(product, productId);
            return product;
        } catch (ProductNotFoundException | RemoteServiceUnavailableException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Erreur de connexion / timeout lors de l'appel au Product Service pour le produit {} : {}", productId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Impossible de contacter le service des produits (délai d'attente dépassé ou indisponibilité réseau).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (RestClientResponseException ex) {
            log.error("Erreur HTTP reçue du Product Service ({}) pour le produit {} : {}", ex.getStatusCode(), productId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Erreur de communication avec le service des produits.",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de l'appel au Product Service pour le produit {} : {}", productId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Erreur inattendue lors de la consultation du catalogue de produits.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private void validateProduct(ProductDto product, Long productId) {
        if (product == null) {
            throw new RemoteServiceUnavailableException(
                    "Réponse vide reçue du service des produits pour le produit " + productId,
                    HttpStatus.BAD_GATEWAY
            );
        }
        if (product.name() == null || product.name().isBlank()) {
            throw new RemoteServiceUnavailableException(
                    "Données produit incomplètes (nom manquant) pour le produit " + productId,
                    HttpStatus.BAD_GATEWAY
            );
        }
        if (product.price() == null || product.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new RemoteServiceUnavailableException(
                    "Données produit incomplètes (prix manquant ou invalide) pour le produit " + productId,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}
