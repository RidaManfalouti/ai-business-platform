package com.aibi.customer_service.controller;

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
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("POST /api/customers : Création valide -> 201 Created")
    void shouldCreateCustomerSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "firstName": "Thomas",
                    "lastName": "Bernard_%s",
                    "email": "thomas.%s@example.com",
                    "phone": "+33612345678",
                    "company": "Tech Innov"
                }
                """.formatted(suffix, suffix);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.firstName").value("Thomas"))
                .andExpect(jsonPath("$.lastName").value("Bernard_" + suffix))
                .andExpect(jsonPath("$.email").value("thomas." + suffix + "@example.com"))
                .andExpect(jsonPath("$.phone").value("+33612345678"))
                .andExpect(jsonPath("$.company").value("Tech Innov"));
    }

    @Test
    @DisplayName("POST /api/customers : Prénom manquant -> 400 Bad Request")
    void shouldReturn400WhenFirstNameIsBlank() throws Exception {
        String payload = """
                {
                    "firstName": "   ",
                    "lastName": "Durand",
                    "email": "test.%s@example.com"
                }
                """.formatted(randomSuffix());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.firstName", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/customers : Nom de famille manquant -> 400 Bad Request")
    void shouldReturn400WhenLastNameIsBlank() throws Exception {
        String payload = """
                {
                    "firstName": "Claire",
                    "lastName": "",
                    "email": "test.%s@example.com"
                }
                """.formatted(randomSuffix());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.lastName", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/customers : Format email invalide -> 400 Bad Request")
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        String payload = """
                {
                    "firstName": "Claire",
                    "lastName": "Durand",
                    "email": "invalid-email-format"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.email", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/customers : Email déjà utilisé -> 409 Conflict")
    void shouldReturn409WhenCustomerWithSameEmailExists() throws Exception {
        String suffix = randomSuffix();
        String email = "duplicate.%s@example.com".formatted(suffix);
        String payload = """
                {
                    "firstName": "Marc",
                    "lastName": "Lemoine",
                    "email": "%s"
                }
                """.formatted(email);

        // Première création réussie
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Deuxième création avec le même email
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("existe déjà")));
    }

    @Test
    @DisplayName("GET /api/customers/{id} : Récupération d'un client existant -> 200 OK")
    void shouldGetCustomerByIdSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "firstName": "Sophie",
                    "lastName": "Rousseau",
                    "email": "sophie.%s@example.com"
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idStr = response.replaceAll(".*\"id\":(\\d+).*", "$1");
        long id = Long.parseLong(idStr);

        mockMvc.perform(get("/api/customers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("Sophie"))
                .andExpect(jsonPath("$.lastName").value("Rousseau"));
    }

    @Test
    @DisplayName("GET /api/customers/{id} : Client inexistant -> 404 Not Found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        mockMvc.perform(get("/api/customers/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    @DisplayName("GET /api/customers : Pagination et tri des clients")
    void shouldListCustomersWithPaginationAndSorting() throws Exception {
        String suffix = randomSuffix();
        for (int i = 1; i <= 3; i++) {
            String payload = """
                    {
                        "firstName": "User_%s",
                        "lastName": "LastName_%d",
                        "email": "user.%s.%d@example.com"
                    }
                    """.formatted(suffix, i, suffix, i);
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/customers?page=0&size=2&sort=id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("GET /api/customers/search?name={q} : Recherche par nom ou prénom")
    void shouldSearchCustomersByName() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "firstName": "SearchTarget_%s",
                    "lastName": "TargetFamily",
                    "email": "target.%s@example.com"
                }
                """.formatted(suffix, suffix);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers/search?name=SearchTarget_" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("SearchTarget_" + suffix));
    }

    @Test
    @DisplayName("GET /api/customers/search?email={q} : Recherche par email")
    void shouldSearchCustomersByEmail() throws Exception {
        String suffix = randomSuffix();
        String email = "emailsearch.%s@example.com".formatted(suffix);
        String payload = """
                {
                    "firstName": "Email",
                    "lastName": "Searcher",
                    "email": "%s"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers/search?email=emailsearch." + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value(email));
    }

    @Test
    @DisplayName("PUT /api/customers/{id} : Modification valide -> 200 OK")
    void shouldUpdateCustomerSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "firstName": "InitialName",
                    "lastName": "InitialLast",
                    "email": "initial.%s@example.com"
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idStr = response.replaceAll(".*\"id\":(\\d+).*", "$1");
        long id = Long.parseLong(idStr);

        String updatePayload = """
                {
                    "firstName": "UpdatedName",
                    "lastName": "UpdatedLast",
                    "email": "updated.%s@example.com",
                    "phone": "+33698765432",
                    "company": "Updated Corp"
                }
                """.formatted(suffix);

        mockMvc.perform(put("/api/customers/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("UpdatedName"))
                .andExpect(jsonPath("$.lastName").value("UpdatedLast"))
                .andExpect(jsonPath("$.email").value("updated." + suffix + "@example.com"))
                .andExpect(jsonPath("$.company").value("Updated Corp"));
    }

    @Test
    @DisplayName("DELETE /api/customers/{id} : Suppression valide -> 204 No Content")
    void shouldDeleteCustomerSuccessfully() throws Exception {
        String suffix = randomSuffix();
        String payload = """
                {
                    "firstName": "ToDelete",
                    "lastName": "User",
                    "email": "delete.%s@example.com"
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idStr = response.replaceAll(".*\"id\":(\\d+).*", "$1");
        long id = Long.parseLong(idStr);

        mockMvc.perform(delete("/api/customers/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers/" + id))
                .andExpect(status().isNotFound());
    }
}
