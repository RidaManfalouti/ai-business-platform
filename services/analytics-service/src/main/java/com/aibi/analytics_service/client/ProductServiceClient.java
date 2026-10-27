package com.aibi.analytics_service.client;

import com.aibi.analytics_service.client.dto.ProductPageDto;
import com.aibi.analytics_service.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClient.class);
    private final RestClient restClient;

    public ProductServiceClient(@Qualifier("productRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ProductPageDto getProductsSortedByStock(int limit) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/products")
                            .queryParam("page", 0)
                            .queryParam("size", limit)
                            .queryParam("sort", "stockQuantity,asc")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new DownstreamServiceException(
                                "Le service des produits a retourné une erreur interne.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(ProductPageDto.class);
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai dépassé pour product-service : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Impossible de joindre le catalogue de produits (délai dépassé ou service indisponible).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel au service des produits : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur de communication avec le catalogue de produits.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public long getTotalProductsCount() {
        try {
            ProductPageDto page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/products")
                            .queryParam("page", 0)
                            .queryParam("size", 1)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new DownstreamServiceException(
                                "Le service des produits a retourné une erreur interne.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(ProductPageDto.class);
            return page != null ? page.totalElements() : 0L;
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai dépassé pour product-service (total count) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Impossible de joindre le catalogue de produits.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (Exception ex) {
            log.error("Erreur lors de la récupération du nombre total de produits : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur de communication avec le catalogue de produits.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}
