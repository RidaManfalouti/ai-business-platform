package com.aibi.ai_service.tools.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CustomerClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerClient.class);

    private final RestClient customerRestClient;

    public CustomerClient(@Qualifier("customerRestClient") RestClient customerRestClient) {
        this.customerRestClient = customerRestClient;
    }

    public String getCustomers(int limit) {
        try {
            return customerRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/customers")
                            .queryParam("size", limit)
                            .queryParam("sort", "id,desc")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à customer-service /api/customers: {}", e.getMessage());
            return "{\"error\":\"DOWNSTREAM_SERVICE_UNAVAILABLE\",\"message\":\"Impossible de récupérer les clients.\"}";
        }
    }
}
