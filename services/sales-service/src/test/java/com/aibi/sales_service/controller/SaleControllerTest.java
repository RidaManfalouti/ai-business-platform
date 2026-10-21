package com.aibi.sales_service.controller;

import com.aibi.sales_service.client.CustomerServiceClient;
import com.aibi.sales_service.client.ProductServiceClient;
import com.aibi.sales_service.client.dto.CustomerDto;
import com.aibi.sales_service.client.dto.ProductDto;
import com.aibi.sales_service.entity.Sale;
import com.aibi.sales_service.entity.SaleItem;
import com.aibi.sales_service.exception.CustomerNotFoundException;
import com.aibi.sales_service.exception.ProductNotFoundException;
import com.aibi.sales_service.exception.RemoteServiceUnavailableException;
import com.aibi.sales_service.model.SaleStatus;
import com.aibi.sales_service.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SaleRepository saleRepository;

    @MockitoBean
    private ProductServiceClient productServiceClient;

    @MockitoBean
    private CustomerServiceClient customerServiceClient;

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void cleanUp() {
        saleRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/sales : Création valide -> 201 Created avec recalcul serveur")
    void shouldCreateSaleSuccessfully() throws Exception {
        Long customerId = 10L;
        Long productId = 100L;

        when(customerServiceClient.getCustomerById(customerId))
                .thenReturn(new CustomerDto(customerId, "Claire", "Dupont", "claire@example.com", "0601020304", "TechCorp"));
        when(productServiceClient.getProductById(productId))
                .thenReturn(new ProductDto(productId, "Écran 27\"", "Moniteur 4K", new BigDecimal("350.00"), 20, "Matériel"));

        String ref = "CMD-" + randomSuffix();
        String payload = """
                {
                    "customerId": %d,
                    "reference": "%s",
                    "items": [
                        {
                            "productId": %d,
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(customerId, ref, productId);

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.reference").value(ref))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(700.00))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productNameSnapshot").value("Écran 27\""))
                .andExpect(jsonPath("$.items[0].unitPrice").value(350.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(700.00));
    }

    @Test
    @DisplayName("POST /api/sales : customerId manquant -> 400 Bad Request")
    void shouldReturn400WhenCustomerIdIsMissing() throws Exception {
        String payload = """
                {
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 1
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.customerId", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/sales : items vide -> 400 Bad Request")
    void shouldReturn400WhenItemsIsEmpty() throws Exception {
        String payload = """
                {
                    "customerId": 1,
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.items", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/sales : quantité nulle ou négative -> 400 Bad Request")
    void shouldReturn400WhenQuantityIsInvalid() throws Exception {
        String payload = """
                {
                    "customerId": 1,
                    "items": [
                        {
                            "productId": 1,
                            "quantity": -2
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/sales : client distant inexistant -> 404 Not Found")
    void shouldReturn404WhenCustomerDoesNotExist() throws Exception {
        when(customerServiceClient.getCustomerById(eq(999L)))
                .thenThrow(new CustomerNotFoundException("Client introuvable avec l'identifiant : 999"));

        String payload = """
                {
                    "customerId": 999,
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 1
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Client introuvable avec l'identifiant : 999"));
    }

    @Test
    @DisplayName("POST /api/sales : produit distant inexistant -> 404 Not Found")
    void shouldReturn404WhenProductDoesNotExist() throws Exception {
        when(customerServiceClient.getCustomerById(eq(1L)))
                .thenReturn(new CustomerDto(1L, "Alice", "Test", "a@test.com", "0102030405", "Co"));
        when(productServiceClient.getProductById(eq(999L)))
                .thenThrow(new ProductNotFoundException("Produit introuvable avec l'identifiant : 999"));

        String payload = """
                {
                    "customerId": 1,
                    "items": [
                        {
                            "productId": 999,
                            "quantity": 1
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Produit introuvable avec l'identifiant : 999"));
    }

    @Test
    @DisplayName("POST /api/sales : stock insuffisant -> 409 Conflict")
    void shouldReturn409WhenStockIsInsufficient() throws Exception {
        when(customerServiceClient.getCustomerById(eq(1L)))
                .thenReturn(new CustomerDto(1L, "Alice", "Test", "a@test.com", "0102030405", "Co"));
        when(productServiceClient.getProductById(eq(10L)))
                .thenReturn(new ProductDto(10L, "Souris", "Souris sans fil", new BigDecimal("20.00"), 3, "Accessoires"));

        // Demande de 5 unités alors qu'il n'y en a que 3
        String payload = """
                {
                    "customerId": 1,
                    "items": [
                        {
                            "productId": 10,
                            "quantity": 5
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("Stock insuffisant")));
    }

    @Test
    @DisplayName("POST /api/sales : référence déjà existante -> 409 Conflict")
    void shouldReturn409WhenReferenceAlreadyExists() throws Exception {
        String existingRef = "REF-DOUBLE-" + randomSuffix();

        Sale existingSale = Sale.builder()
                .reference(existingRef)
                .customerId(1L)
                .status(SaleStatus.PENDING)
                .totalAmount(new BigDecimal("50.00"))
                .build();
        saleRepository.save(existingSale);

        String payload = """
                {
                    "customerId": 1,
                    "reference": "%s",
                    "items": [
                        {
                            "productId": 10,
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(existingRef);

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("existe déjà")));
    }

    @Test
    @DisplayName("POST /api/sales : panne d'un service distant -> 502 Bad Gateway / 503 Service Unavailable")
    void shouldReturn502Or503WhenRemoteServiceFails() throws Exception {
        when(customerServiceClient.getCustomerById(eq(1L)))
                .thenThrow(new RemoteServiceUnavailableException("Le service client est indisponible", HttpStatus.BAD_GATEWAY));

        String payload = """
                {
                    "customerId": 1,
                    "items": [
                        {
                            "productId": 10,
                            "quantity": 1
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.message").value("Le service client est indisponible"));
    }

    @Test
    @DisplayName("GET /api/sales/{id} : Vente existante -> 200 OK avec ses lignes")
    void shouldGetSaleByIdSuccessfully() throws Exception {
        String ref = "REF-GET-" + randomSuffix();
        Sale sale = Sale.builder()
                .reference(ref)
                .customerId(2L)
                .status(SaleStatus.PENDING)
                .totalAmount(new BigDecimal("150.00"))
                .build();

        SaleItem item = SaleItem.builder()
                .productId(50L)
                .productNameSnapshot("Clavier Mécanique")
                .quantity(1)
                .unitPrice(new BigDecimal("150.00"))
                .lineTotal(new BigDecimal("150.00"))
                .build();
        sale.addItem(item);

        Sale saved = saleRepository.save(sale);

        mockMvc.perform(get("/api/sales/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.reference").value(ref))
                .andExpect(jsonPath("$.customerId").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productNameSnapshot").value("Clavier Mécanique"));
    }

    @Test
    @DisplayName("GET /api/sales/{id} : Vente inexistante -> 404 Not Found")
    void shouldReturn404WhenSaleDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/sales/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/sales : Liste paginée et triée -> 200 OK")
    void shouldGetAllSalesPaged() throws Exception {
        for (int i = 1; i <= 3; i++) {
            Sale s = Sale.builder()
                    .reference("REF-PAGE-" + i + "-" + randomSuffix())
                    .customerId((long) i)
                    .status(SaleStatus.PENDING)
                    .totalAmount(new BigDecimal(i * 10))
                    .build();
            saleRepository.save(s);
        }

        mockMvc.perform(get("/api/sales?page=0&size=2&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("GET /api/sales/customer/{customerId} : Ventes d'un client -> 200 OK")
    void shouldGetSalesByCustomerId() throws Exception {
        Long customerId = 42L;

        Sale s1 = Sale.builder()
                .reference("REF-CUST-1-" + randomSuffix())
                .customerId(customerId)
                .status(SaleStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        Sale s2 = Sale.builder()
                .reference("REF-CUST-2-" + randomSuffix())
                .customerId(customerId)
                .status(SaleStatus.CONFIRMED)
                .totalAmount(new BigDecimal("200.00"))
                .build();
        saleRepository.save(s1);
        saleRepository.save(s2);

        mockMvc.perform(get("/api/sales/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].customerId").value(customerId))
                .andExpect(jsonPath("$.content[1].customerId").value(customerId));
    }

    @Test
    @DisplayName("PATCH /api/sales/{id}/status : Transition autorisée PENDING -> CONFIRMED -> 200 OK")
    void shouldUpdateStatusSuccessfully() throws Exception {
        Sale sale = Sale.builder()
                .reference("REF-STATUS-" + randomSuffix())
                .customerId(1L)
                .status(SaleStatus.PENDING)
                .totalAmount(new BigDecimal("80.00"))
                .build();
        Sale saved = saleRepository.save(sale);

        String payload = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/api/sales/{id}/status", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PATCH /api/sales/{id}/status : Transition interdite COMPLETED -> CANCELLED -> 409 Conflict")
    void shouldReturn409WhenTransitionIsForbidden() throws Exception {
        Sale sale = Sale.builder()
                .reference("REF-FORBIDDEN-" + randomSuffix())
                .customerId(1L)
                .status(SaleStatus.COMPLETED)
                .totalAmount(new BigDecimal("120.00"))
                .build();
        Sale saved = saleRepository.save(sale);

        String payload = """
                {
                    "status": "CANCELLED"
                }
                """;

        mockMvc.perform(patch("/api/sales/{id}/status", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("Transition de statut interdite")));
    }

    @Test
    @DisplayName("PATCH /api/sales/{id}/status : Valeur de statut invalide -> 400 Bad Request")
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        Sale sale = Sale.builder()
                .reference("REF-INV-STATUS-" + randomSuffix())
                .customerId(1L)
                .status(SaleStatus.PENDING)
                .totalAmount(new BigDecimal("10.00"))
                .build();
        Sale saved = saleRepository.save(sale);

        String payload = """
                {
                    "status": "STATUT_INEXISTANT"
                }
                """;

        mockMvc.perform(patch("/api/sales/{id}/status", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("PATCH /api/sales/{id}/status : Vente inexistante -> 404 Not Found")
    void shouldReturn404WhenUpdatingNonExistentSale() throws Exception {
        String payload = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/api/sales/999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
