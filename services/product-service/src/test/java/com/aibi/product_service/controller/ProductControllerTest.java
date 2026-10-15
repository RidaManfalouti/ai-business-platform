package com.aibi.product_service.controller;

import com.aibi.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("POST /api/products : Création valide -> 201 Created")
    void shouldCreateProductSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "name": "Ecran 4K %s",
                    "description": "Moniteur Ultra HD 27 pouces",
                    "price": 349.99,
                    "stockQuantity": 12,
                    "category": "Ecrans"
                }
                """.formatted(suffix);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Ecran 4K " + suffix))
                .andExpect(jsonPath("$.price").value(349.99))
                .andExpect(jsonPath("$.stockQuantity").value(12))
                .andExpect(jsonPath("$.category").value("Ecrans"));
    }

    @Test
    @DisplayName("POST /api/products : Nom manquant ou vide -> 400 Bad Request")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        String payload = """
                {
                    "name": "   ",
                    "description": "Description",
                    "price": 49.99,
                    "stockQuantity": 5,
                    "category": "Divers"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.name", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/products : Prix négatif -> 400 Bad Request")
    void shouldReturn400WhenPriceIsNegative() throws Exception {
        String payload = """
                {
                    "name": "Produit Prix Negatif %s",
                    "description": "Description",
                    "price": -10.00,
                    "stockQuantity": 5,
                    "category": "Divers"
                }
                """.formatted(randomSuffix());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.price", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/products : Stock négatif -> 400 Bad Request")
    void shouldReturn400WhenStockIsNegative() throws Exception {
        String payload = """
                {
                    "name": "Produit Stock Negatif %s",
                    "description": "Description",
                    "price": 19.99,
                    "stockQuantity": -3,
                    "category": "Divers"
                }
                """.formatted(randomSuffix());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.stockQuantity", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/products : Nom déjà existant -> 409 Conflict")
    void shouldReturn409WhenProductNameAlreadyExists() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "name": "Souris Gaming %s",
                    "description": "Souris 16000 DPI",
                    "price": 59.90,
                    "stockQuantity": 20,
                    "category": "Périphériques"
                }
                """.formatted(suffix);

        // Première création
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Deuxième création avec le même nom
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("existe déjà")));
    }

    @Test
    @DisplayName("GET /api/products/{id} : Produit existant -> 200 OK")
    void shouldGetProductByIdSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "name": "Casque Audio %s",
                    "description": "Casque sans fil ANC",
                    "price": 199.00,
                    "stockQuantity": 8,
                    "category": "Audio"
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Récupération de l'id
        String idStr = response.replaceAll(".*\"id\":(\\d+).*", "$1");
        long id = Long.parseLong(idStr);

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Casque Audio " + suffix));
    }

    @Test
    @DisplayName("GET /api/products/{id} : Produit inexistant -> 404 Not Found")
    void shouldReturn404WhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    @DisplayName("GET /api/products : Pagination et tri")
    void shouldListProductsWithPaginationAndSorting() throws Exception {
        String suffix = randomSuffix();
        for (int i = 1; i <= 3; i++) {
            String payload = """
                    {
                        "name": "Item %s %d",
                        "price": %d.00,
                        "stockQuantity": 10,
                        "category": "Test"
                    }
                    """.formatted(suffix, i, i * 10);
            mockMvc.perform(post("/api/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/products?page=0&size=2&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("GET /api/products/search : Recherche par nom")
    void shouldSearchProductsByName() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "name": "Microphone Studio %s",
                    "price": 149.00,
                    "stockQuantity": 4,
                    "category": "Audio"
                }
                """.formatted(suffix);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products/search?name=Microphone Studio " + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Microphone Studio " + suffix));
    }

    @Test
    @DisplayName("PUT /api/products/{id} : Modification valide -> 200 OK")
    void shouldUpdateProductSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "name": "Webcam HD %s",
                    "price": 79.90,
                    "stockQuantity": 15,
                    "category": "Vidéo"
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idStr = response.replaceAll(".*\"id\":(\\d+).*", "$1");
        long id = Long.parseLong(idStr);

        String updatePayload = """
                {
                    "name": "Webcam 4K Pro %s",
                    "description": "Nouvelle version 4K 60fps",
                    "price": 119.90,
                    "stockQuantity": 25,
                    "category": "Vidéo"
                }
                """.formatted(suffix);

        mockMvc.perform(put("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Webcam 4K Pro " + suffix))
                .andExpect(jsonPath("$.price").value(119.90))
                .andExpect(jsonPath("$.stockQuantity").value(25));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} : Suppression valide -> 204 No Content")
    void shouldDeleteProductSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "name": "Câble HDMI %s",
                    "price": 14.90,
                    "stockQuantity": 50,
                    "category": "Câbles"
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idStr = response.replaceAll(".*\"id\":(\\d+).*", "$1");
        long id = Long.parseLong(idStr);

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound());
    }
}
