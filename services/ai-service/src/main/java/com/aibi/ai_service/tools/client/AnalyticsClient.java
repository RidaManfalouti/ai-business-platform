package com.aibi.ai_service.tools.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AnalyticsClient {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsClient.class);

    private final RestClient analyticsRestClient;

    public AnalyticsClient(@Qualifier("analyticsRestClient") RestClient analyticsRestClient) {
        this.analyticsRestClient = analyticsRestClient;
    }

    public String getKpiSummary(String from, String to, String timezone) {
        try {
            return analyticsRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/analytics/kpi")
                            .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                            .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                            .queryParamIfPresent("timezone", java.util.Optional.ofNullable(timezone))
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à analytics-service /kpi: {}", e.getMessage());
            return "{\"error\":\"DOWNSTREAM_SERVICE_UNAVAILABLE\",\"message\":\"Impossible de récupérer les KPIs actuellement.\"}";
        }
    }

    public String getTrends(String from, String to, String timezone) {
        try {
            return analyticsRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/analytics/trends")
                            .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                            .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                            .queryParamIfPresent("timezone", java.util.Optional.ofNullable(timezone))
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à analytics-service /trends: {}", e.getMessage());
            return "{\"error\":\"DOWNSTREAM_SERVICE_UNAVAILABLE\",\"message\":\"Impossible de récupérer les tendances.\"}";
        }
    }

    public String getTopProducts(String from, String to, String timezone, int limit) {
        try {
            return analyticsRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/analytics/top-products")
                            .queryParamIfPresent("from", java.util.Optional.ofNullable(from))
                            .queryParamIfPresent("to", java.util.Optional.ofNullable(to))
                            .queryParamIfPresent("timezone", java.util.Optional.ofNullable(timezone))
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à analytics-service /top-products: {}", e.getMessage());
            return "{\"error\":\"DOWNSTREAM_SERVICE_UNAVAILABLE\",\"message\":\"Impossible de récupérer les top produits.\"}";
        }
    }

    public String getLowStock(Integer threshold, Integer limit) {
        try {
            return analyticsRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/analytics/low-stock")
                            .queryParamIfPresent("threshold", java.util.Optional.ofNullable(threshold))
                            .queryParamIfPresent("limit", java.util.Optional.ofNullable(limit))
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à analytics-service /low-stock: {}", e.getMessage());
            return "{\"error\":\"DOWNSTREAM_SERVICE_UNAVAILABLE\",\"message\":\"Impossible de vérifier les stocks faibles.\"}";
        }
    }
}
