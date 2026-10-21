package com.aibi.sales_service.client;

import com.aibi.sales_service.client.dto.ProductDto;
import com.aibi.sales_service.exception.ProductNotFoundException;
import com.aibi.sales_service.exception.RemoteServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClient.class);
    private final RestClient restClient;

    public ProductServiceClient(@Qualifier("productRestClient") RestClient restClient) {
        this.restClient = restClient;
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
