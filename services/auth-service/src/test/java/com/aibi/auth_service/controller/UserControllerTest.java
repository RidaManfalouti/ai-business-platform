package com.aibi.auth_service.controller;

import com.aibi.auth_service.entity.User;
import com.aibi.auth_service.repository.UserRepository;
import com.aibi.auth_service.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Value("${jwt.secret}")
    private String jwtSecret;

    private User getOrCreateUser(String username, String email, User.Role role) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .password("$2a$10$mbRvYPH8YijtKb6Nqw9fA.lhID2ZEQnro9X6qqJ7ssoouyzZsoiMe")
                        .role(role)
                        .build())
        );
    }

    private String createRawTokenWithoutRoles(String email) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldReturn401WhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturn200WhenTokenIsValid() throws Exception {
        String email = "testuser01@example.com";
        getOrCreateUser("testuser01", email, User.Role.USER);

        String token = jwtService.generateToken(email, "USER");

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.message").value("Authentification JWT réussie"));
    }

    @Test
    @DisplayName("USER tentant d'accéder à la liste des utilisateurs -> 403 Forbidden")
    void shouldReturn403WhenUserAttemptsToGetAllUsers() throws Exception {
        String email = "regular_user_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        getOrCreateUser("reg_" + UUID.randomUUID().toString().substring(0, 8), email, User.Role.USER);

        String userToken = jwtService.generateToken(email, "USER");

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Accès refusé. Droits insuffisants."));
    }

    @Test
    @DisplayName("ADMIN accédant à la liste des utilisateurs -> 200 OK")
    void shouldReturn200WhenAdminAttemptsToGetAllUsers() throws Exception {
        String email = "admin_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        getOrCreateUser("adm_" + UUID.randomUUID().toString().substring(0, 8), email, User.Role.ADMIN);

        String adminToken = jwtService.generateToken(email, "ADMIN");

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(Iterable.class)));
    }

    @Test
    @DisplayName("USER tentant de modifier le rôle d'un utilisateur -> 403 Forbidden")
    void shouldReturn403WhenUserAttemptsToChangeRole() throws Exception {
        String userEmail = "normal_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        User target = getOrCreateUser("tgt_" + UUID.randomUUID().toString().substring(0, 8), userEmail, User.Role.USER);

        String userToken = jwtService.generateToken(userEmail, "USER");

        mockMvc.perform(patch("/api/users/" + target.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("ADMIN modifiant le rôle d'un utilisateur -> 200 OK")
    void shouldAllowAdminToChangeUserRole() throws Exception {
        String adminEmail = "superadmin_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        getOrCreateUser("sadmin_" + UUID.randomUUID().toString().substring(0, 8), adminEmail, User.Role.ADMIN);

        String targetEmail = "promoteme_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        User targetUser = getOrCreateUser("promote_" + UUID.randomUUID().toString().substring(0, 8), targetEmail, User.Role.USER);

        String adminToken = jwtService.generateToken(adminEmail, "ADMIN");

        mockMvc.perform(patch("/api/users/" + targetUser.getId() + "/role")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Token sans claim roles accédant à endpoint avec rôle -> 403 Forbidden")
    void shouldReturn403WhenRolesClaimIsMissingOnRoleProtectedEndpoint() throws Exception {
        String email = "noroles_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        getOrCreateUser("noroles_" + UUID.randomUUID().toString().substring(0, 8), email, User.Role.USER);

        String tokenWithoutRoles = createRawTokenWithoutRoles(email);

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutRoles))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
