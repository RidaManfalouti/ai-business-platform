package com.aibi.analytics_service.client;

import com.aibi.analytics_service.client.dto.CustomerPageDto;
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
public class CustomerServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceClient.class);
    private final RestClient restClient;

    public CustomerServiceClient(@Qualifier("customerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public long getTotalCustomersCount() {
        try {
            CustomerPageDto page = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/customers")
                            .queryParam("page", 0)
                            .queryParam("size", 1)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new DownstreamServiceException(
                                "Le service client a retourné une erreur interne.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(CustomerPageDto.class);
            return page != null ? page.totalElements() : 0L;
        } catch (DownstreamServiceException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Délai d'attente dépassé pour customer-service : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Impossible de joindre le service client (délai dépassé ou service indisponible).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel au service client : {}", ex.getMessage());
            throw new DownstreamServiceException(
                    "Erreur de communication avec le service client.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}
