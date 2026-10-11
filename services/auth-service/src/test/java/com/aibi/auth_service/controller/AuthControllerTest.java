package com.aibi.auth_service.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String generateUniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("Inscription valide : HTTP 201 et champs de réponse attendus")
    void shouldRegisterSuccessfully() throws Exception {
        String suffix = generateUniqueSuffix();
        String username = "user_" + suffix;
        String email = "user_" + suffix + "@example.com";
        String password = "SecurePassword123!";

        String payload = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("Inscription avec email déjà utilisé : HTTP 409 Conflict")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        String suffix = generateUniqueSuffix();
        String username = "user_" + suffix;
        String email = "dup_" + suffix + "@example.com";
        String password = "SecurePassword123!";

        String payload = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password);

        // Première inscription réussie
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Deuxième inscription avec le même email mais un nom d'utilisateur différent
        String duplicatePayload = """
                {
                    "username": "other_%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(suffix, email, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicatePayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Cet email est déjà utilisé."));
    }

    @Test
    @DisplayName("Inscription avec un email invalide : HTTP 400 Bad Request")
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        String suffix = generateUniqueSuffix();
        String username = "user_" + suffix;
        String invalidEmail = "invalid-email-format";
        String password = "SecurePassword123!";

        String payload = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, invalidEmail, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.messages.email", notNullValue()));
    }

    @Test
    @DisplayName("Connexion avec identifiants valides : HTTP 200 OK et JWT")
    void shouldLoginSuccessfully() throws Exception {
        String suffix = generateUniqueSuffix();
        String username = "user_" + suffix;
        String email = "login_" + suffix + "@example.com";
        String password = "SecurePassword123!";

        String registerPayload = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password);

        // Inscription préalable
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        // Connexion avec les mêmes identifiants
        String loginPayload = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("Connexion avec mot de passe incorrect : HTTP 401 Unauthorized")
    void shouldReturn401WhenPasswordIsIncorrect() throws Exception {
        String suffix = generateUniqueSuffix();
        String username = "user_" + suffix;
        String email = "wrongpass_" + suffix + "@example.com";
        String password = "SecurePassword123!";

        String registerPayload = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(username, email, password);

        // Inscription préalable
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        // Tentative de connexion avec un mauvais mot de passe
        String badLoginPayload = """
                {
                    "email": "%s",
                    "password": "WrongPassword999!"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
    }
}
