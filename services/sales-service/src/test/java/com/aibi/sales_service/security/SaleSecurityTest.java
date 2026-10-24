package com.aibi.sales_service.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String createToken(String subject, long validityMillis) {
        return createToken(subject, List.of("ROLE_USER"), validityMillis);
    }

    private String createToken(String subject, Object rolesClaim, long validityMillis) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
            JWSSigner signer = new MACSigner(secretBytes);

            long now = System.currentTimeMillis();
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + validityMillis));

            if (rolesClaim != null) {
                builder.claim("roles", rolesClaim);
            }

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Requête sans token sur endpoint protégé -> 401 Unauthorized")
    void shouldReturn401WhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentification requise ou token invalide."));
    }

    @Test
    @DisplayName("Requête avec token invalide ou corrompu -> 401 Unauthorized")
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentification requise ou token invalide."));
    }

    @Test
    @DisplayName("Requête avec token signé par une mauvaise clé -> 401 Unauthorized")
    void shouldReturn401WhenTokenSignedWithWrongKey() throws Exception {
        byte[] wrongKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            wrongKey[i] = (byte) i;
        }
        JWSSigner signer = new MACSigner(wrongKey);
        SignedJWT wrongSigned = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject("user@example.com")
                        .claim("roles", List.of("ROLE_USER"))
                        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                        .build()
        );
        wrongSigned.sign(signer);

        mockMvc.perform(get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongSigned.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentification requise ou token invalide."));
    }

    @Test
    @DisplayName("Requête avec token expiré -> 401 Unauthorized")
    void shouldReturn401WhenTokenIsExpired() throws Exception {
        String expiredToken = createToken("user@example.com", -60000);

        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentification requise ou token invalide."));
    }

    @Test
    @DisplayName("Requête avec token valide (ROLE_USER) -> 200 OK")
    void shouldReturn200WhenTokenIsValid() throws Exception {
        String validToken = createToken("validuser@example.com", 3600000);

        mockMvc.perform(get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requête avec token valide (ROLE_ADMIN) -> 200 OK")
    void shouldReturn200WhenAdminTokenIsValid() throws Exception {
        String adminToken = createToken("admin@example.com", List.of("ROLE_ADMIN"), 3600000);

        mockMvc.perform(get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token sans claim roles accédant à endpoint protégé -> 403 Forbidden")
    void shouldReturn403WhenRolesClaimIsMissing() throws Exception {
        String tokenNoRoles = createToken("noroles@example.com", null, 3600000);

        mockMvc.perform(get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenNoRoles))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Token avec claim roles malformé -> 403 Forbidden")
    void shouldReturn403WhenRoleIsMalformed() throws Exception {
        String malformedToken = createToken("weird@example.com", List.of("UNKNOWN_ROLE"), 3600000);

        mockMvc.perform(get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + malformedToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Endpoint public /actuator/health accessible sans token -> 200 OK")
    void shouldAllowPublicAccessToActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint public /v3/api-docs accessible sans token -> 200 OK")
    void shouldAllowPublicAccessToApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
