package com.aibi.auth_service.controller;

import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.UserRepository;
import com.aibi.auth_service.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturn401WhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturn200WhenTokenIsValid() throws Exception {
        String email = "testuser01@example.com";

        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(User.builder()
                    .username("testuser01")
                    .email(email)
                    .password("$2a$10$mbRvYPH8YijtKb6Nqw9fA.lhID2ZEQnro9X6qqJ7ssoouyzZsoiMe")
                    .role(User.Role.USER)
                    .build());
        }

        // Génère un JWT signé avec la clé configurée
        String token = jwtService.generateToken(email);

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.message")
                        .value("Authentification JWT réussie"));
    }
}
