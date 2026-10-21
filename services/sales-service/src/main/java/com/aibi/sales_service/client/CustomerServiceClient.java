package com.aibi.sales_service.client;

import com.aibi.sales_service.client.dto.CustomerDto;
import com.aibi.sales_service.exception.CustomerNotFoundException;
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

@Component
public class CustomerServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceClient.class);
    private final RestClient restClient;

    public CustomerServiceClient(@Qualifier("customerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public CustomerDto getCustomerById(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("L'identifiant du client ne peut être nul.");
        }

        try {
            CustomerDto customer = restClient.get()
                    .uri("/api/customers/{id}", customerId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new CustomerNotFoundException("Client introuvable avec l'identifiant : " + customerId);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new RemoteServiceUnavailableException(
                                "Le service client est temporairement indisponible.",
                                HttpStatus.BAD_GATEWAY
                        );
                    })
                    .body(CustomerDto.class);

            validateCustomer(customer, customerId);
            return customer;
        } catch (CustomerNotFoundException | RemoteServiceUnavailableException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Erreur de connexion / timeout lors de l'appel au Customer Service pour le client {} : {}", customerId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Impossible de contacter le service client (délai d'attente dépassé ou indisponibilité réseau).",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        } catch (RestClientResponseException ex) {
            log.error("Erreur HTTP reçue du Customer Service ({}) pour le client {} : {}", ex.getStatusCode(), customerId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Erreur de communication avec le service client.",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de l'appel au Customer Service pour le client {} : {}", customerId, ex.getMessage());
            throw new RemoteServiceUnavailableException(
                    "Erreur inattendue lors de la vérification du client.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private void validateCustomer(CustomerDto customer, Long customerId) {
        if (customer == null) {
            throw new RemoteServiceUnavailableException(
                    "Réponse vide reçue du service client pour le client " + customerId,
                    HttpStatus.BAD_GATEWAY
            );
        }
        if (customer.id() == null) {
            throw new RemoteServiceUnavailableException(
                    "Données client incomplètes reçues pour le client " + customerId,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}
