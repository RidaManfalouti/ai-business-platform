package com.aibi.sales_service.client;

import com.aibi.sales_service.client.dto.*;
import com.aibi.sales_service.exception.InsufficientStockException;
import com.aibi.sales_service.exception.ProductNotFoundException;
import com.aibi.sales_service.exception.RemoteServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProductServiceClientTest {

    private MockRestServiceServer mockServer;
    private ProductServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8082");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        client = new ProductServiceClient(restClient);
    }

    @Test
    @DisplayName("getProductById : Réponse 200 OK valide -> ProductDto")
    void getProductById_Success() {
        String json = """
                {
                    "id": 1,
                    "name": "MacBook Pro M3",
                    "description": "Ordinateur portable",
                    "price": 1999.99,
                    "stockQuantity": 10,
                    "category": "Informatique"
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/products/1"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        ProductDto product = client.getProductById(1L);
        assertThat(product).isNotNull();
        assertThat(product.id()).isEqualTo(1L);
        assertThat(product.name()).isEqualTo("MacBook Pro M3");
        assertThat(product.price()).isEqualByComparingTo(new BigDecimal("1999.99"));
        assertThat(product.stockQuantity()).isEqualTo(10);
        mockServer.verify();
    }

    @Test
    @DisplayName("getProductById : Réponse 404 Not Found -> ProductNotFoundException")
    void getProductById_NotFound() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/99"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Produit introuvable");
        mockServer.verify();
    }

    @Test
    @DisplayName("getProductById : Réponse 500 Erreur Serveur -> RemoteServiceUnavailableException (502)")
    void getProductById_ServerError() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getProductById(1L))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .satisfies(ex -> {
                    RemoteServiceUnavailableException rse = (RemoteServiceUnavailableException) ex;
                    assertThat(rse.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
        mockServer.verify();
    }

    @Test
    @DisplayName("getProductById : Données incomplètes (prix manquant) -> RemoteServiceUnavailableException (502)")
    void getProductById_InvalidData() {
        String invalidJson = """
                {
                    "id": 1,
                    "name": "MacBook Pro M3",
                    "price": null,
                    "stockQuantity": 10
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/products/1"))
                .andRespond(withSuccess(invalidJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getProductById(1L))
                .isInstanceOf(RemoteServiceUnavailableException.class)
                .hasMessageContaining("Données produit incomplètes");
        mockServer.verify();
    }

    @Test
    @DisplayName("getProductById : Identifiant null -> IllegalArgumentException")
    void getProductById_NullId() {
        assertThatThrownBy(() -> client.getProductById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("reserveStock : Réservation réussie 201 Created -> StockReservationResponse")
    void reserveStock_Success() {
        String json = """
                {
                    "reservationId": "RES-001",
                    "status": "RESERVED",
                    "items": [
                        { "productId": 1, "quantity": 2 }
                    ]
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/products/reservations"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(json));

        StockReservationRequest request = new StockReservationRequest("RES-001",
                java.util.List.of(new StockReservationItemRequest(1L, 2)));

        StockReservationResponse response = client.reserveStock(request);
        assertThat(response).isNotNull();
        assertThat(response.reservationId()).isEqualTo("RES-001");
        assertThat(response.status()).isEqualTo("RESERVED");
        mockServer.verify();
    }

    @Test
    @DisplayName("reserveStock : 409 Conflict reçu de Product Service -> InsufficientStockException")
    void reserveStock_InsufficientStock_409() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/reservations"))
                .andRespond(withStatus(HttpStatus.CONFLICT).body("{\"message\":\"Stock insuffisant\"}"));

        StockReservationRequest request = new StockReservationRequest("RES-FAIL",
                java.util.List.of(new StockReservationItemRequest(1L, 100)));

        assertThatThrownBy(() -> client.reserveStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Stock insuffisant");
        mockServer.verify();
    }

    @Test
    @DisplayName("releaseStock : Libération réussie 200 OK -> StockReleaseResponse")
    void releaseStock_Success() {
        String json = """
                {
                    "reservationId": "RES-001",
                    "status": "RELEASED",
                    "message": "Stock libéré avec succès."
                }
                """;

        mockServer.expect(requestTo("http://localhost:8082/api/products/reservations/RES-001/release"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        StockReleaseResponse response = client.releaseStock("RES-001");
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("RELEASED");
        mockServer.verify();
    }

    @Test
    @DisplayName("releaseStock : 404 Not Found reçu de Product Service -> géré sans exception fatale (retourne null)")
    void releaseStock_NotFound_HandledGracefully() {
        mockServer.expect(requestTo("http://localhost:8082/api/products/reservations/RES-NON-EXISTENT/release"))
                .andRespond(withResourceNotFound());

        StockReleaseResponse response = client.releaseStock("RES-NON-EXISTENT");
        assertThat(response).isNull();
        mockServer.verify();
    }

    @Test
    @DisplayName("reserveStock : Réessai idempotent d'une requête en échec réseau aval -> retourne la réservation existante")
    void reserveStock_RetryIdempotent() {
        String json = """
                {
                    "reservationId": "RES-RETRY-001",
                    "status": "RESERVED",
                    "items": [
                        { "productId": 1, "quantity": 2 }
                    ]
                }
                """;

        // Simule deux appels successifs avec le même reservationId (ex: après retry)
        mockServer.expect(requestTo("http://localhost:8082/api/products/reservations"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(json));

        mockServer.expect(requestTo("http://localhost:8082/api/products/reservations"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        StockReservationRequest request = new StockReservationRequest("RES-RETRY-001",
                java.util.List.of(new StockReservationItemRequest(1L, 2)));

        StockReservationResponse res1 = client.reserveStock(request);
        StockReservationResponse res2 = client.reserveStock(request);

        assertThat(res1.reservationId()).isEqualTo("RES-RETRY-001");
        assertThat(res2.reservationId()).isEqualTo("RES-RETRY-001");
        assertThat(res2.status()).isEqualTo("RESERVED");
        mockServer.verify();
    }
}
