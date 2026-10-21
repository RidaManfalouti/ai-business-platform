package com.aibi.sales_service.client;

import com.aibi.sales_service.client.dto.CustomerDto;
import com.aibi.sales_service.exception.CustomerNotFoundException;
import com.aibi.sales_service.exception.RemoteServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CustomerServiceClientTest {

    private MockRestServiceServer mockServer;
    private CustomerServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8083");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        client = new CustomerServiceClient(restClient);
    }

    @Test
    @DisplayName("getCustomerById : Réponse 200 OK valide -> CustomerDto")
    void getCustomerById_Success() {
        String json = """
                {
                    "id": 1,
                    "firstName": "Jean",
                    "lastName": "Dupont",
                    "email": "jean.dupont@example.com",
                    "phone": "+33612345678",
                    "company": "Acme Corp"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8083/api/customers/1"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        CustomerDto customer = client.getCustomerById(1L);
        assertThat(customer).isNotNull();
        assertThat(customer.id()).isEqualTo(1L);
        assertThat(customer.firstName()).isEqualTo("Jean");
        assertThat(customer.lastName()).isEqualTo("Dupont");
        assertThat(customer.email()).isEqualTo("jean.dupont@example.com");
        mockServer.verify();
    }

    @Test
    @DisplayName("getCustomerById : Réponse 404 Not Found -> CustomerNotFoundException")
    void getCustomerById_NotFound() {
        mockServer.expect(requestTo("http://localhost:8083/api/customers/404"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getCustomerById(404L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Client introuvable");
        mockServer.verify();
    }

    @Test
    @DisplayName("getCustomerById : Réponse 500 Erreur Serveur -> RemoteServiceUnavailableException (502)")
    void getCustomerById_ServerError() {
        mockServer.expect(requestTo("http://localhost:8083/api/customers/1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getCustomerById(1L))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .satisfies(ex -> {
                    RemoteServiceUnavailableException rse = (RemoteServiceUnavailableException) ex;
                    assertThat(rse.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
        mockServer.verify();
    }

    @Test
    @DisplayName("getCustomerById : Données incomplètes (id manquant) -> RemoteServiceUnavailableException (502)")
    void getCustomerById_InvalidData() {
        String invalidJson = """
                {
                    "firstName": "Jean"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8083/api/customers/1"))
                .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getCustomerById(1L))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("Données client incomplètes");
        mockServer.verify();
    }

    @Test
    @DisplayName("getCustomerById : Identifiant null -> IllegalArgumentException")
    void getCustomerById_NullId() {
        assertThatThrownBy(() -> client.getCustomerById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
