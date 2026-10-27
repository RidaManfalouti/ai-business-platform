package com.aibi.analytics_service.client;

import com.aibi.analytics_service.client.dto.DailyTrendDto;
import com.aibi.analytics_service.client.dto.SalesStatusCountDto;
import com.aibi.analytics_service.client.dto.SalesSummaryDto;
import com.aibi.analytics_service.client.dto.TopProductDto;
import com.aibi.analytics_service.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SalesServiceClient {

    private static final Logger log = LoggerFactory.getLogger(SalesServiceClient.class);
    private final RestClient restClient;

    public SalesServiceClient(@Qualifier("salesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public SalesSummaryDto getSummaryAggregate(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/sales/aggregates/summary");
                        if (startDate != null) {
                            uriBuilder.queryParam("startDate", startDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        if (endDate != null) {
                            uriBuilder.queryParam("endDate", endDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new DownstreamServiceException(
                                "Le service des ventes a retourné une erreur interne lors du calcul du résumé.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(SalesSummaryDto.class);
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai d'attente dépassé ou réseau injoignable pour sales-service (summary) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Impossible de joindre le service des ventes (délai d'attente dépassé ou service indisponible).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (RestClientResponseException ex) {
            log.error("Erreur HTTP reçue du service des ventes (summary) [{}]: {}", ex.getStatusCode(), ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur de communication avec le service des ventes.",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de l'appel au service des ventes (summary) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur inattendue lors de la communication avec le service des ventes.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public List<SalesStatusCountDto> getStatusDistribution(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/sales/aggregates/status-distribution");
                        if (startDate != null) {
                            uriBuilder.queryParam("startDate", startDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        if (endDate != null) {
                            uriBuilder.queryParam("endDate", endDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new DownstreamServiceException(
                                "Le service des ventes a retourné une erreur interne lors de l'analyse des statuts.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(new ParameterizedTypeReference<List<SalesStatusCountDto>>() {});
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai d'attente dépassé pour sales-service (status-distribution) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Impossible de joindre le service des ventes (délai dépassé).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel au service des ventes (status-distribution) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur de communication avec le service des ventes pour la distribution des statuts.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public List<DailyTrendDto> getDailyTrends(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/sales/aggregates/trends");
                        if (startDate != null) {
                            uriBuilder.queryParam("startDate", startDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        if (endDate != null) {
                            uriBuilder.queryParam("endDate", endDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new DownstreamServiceException(
                                "Le service des ventes a retourné une erreur lors du calcul des tendances.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(new ParameterizedTypeReference<List<DailyTrendDto>>() {});
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai d'attente dépassé pour sales-service (trends) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Impossible de joindre le service des ventes (délai dépassé).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel au service des ventes (trends) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur de communication avec le service des ventes pour les tendances.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    public List<TopProductDto> getTopProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/api/sales/aggregates/top-products");
                        uriBuilder.queryParam("limit", limit);
                        if (startDate != null) {
                            uriBuilder.queryParam("startDate", startDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        if (endDate != null) {
                            uriBuilder.queryParam("endDate", endDate.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new DownstreamServiceException(
                                "Le service des ventes a retourné une erreur lors de l'analyse des meilleures ventes.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(new ParameterizedTypeReference<List<TopProductDto>>() {});
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai d'attente dépassé pour sales-service (top-products) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Impossible de joindre le service des ventes (délai dépassé).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel au service des ventes (top-products) : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur de communication avec le service des ventes pour le classement produits.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}
