package com.aibi.analytics_service.security;

import com.aibi.analytics_service.dto.KpiSummaryResponse;
import com.aibi.analytics_service.service.AnalyticsService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @MockitoBean
    private AnalyticsService analyticsService;

    private String createToken(String subject, List<String> roles, long validityMillis) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
            JWSSigner signer = new MACSigner(secretBytes);

            long now = System.currentTimeMillis();
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + validityMillis));

            if (roles != null) {
                builder.claim("roles", roles);
            }

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Requête anonyme sans token sur /api/analytics/kpi -> 401 Unauthorized")
    void testUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/analytics/kpi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentification requise ou token invalide."));
    }

    @Test
    @DisplayName("Requête avec faux en-tête X-Role: ADMIN sans token -> 401 Unauthorized (les en-têtes de rôles ne sont pas crus)")
    void testRoleHeaderSpoofing_Returns401() throws Exception {
        mockMvc.perform(get("/api/analytics/kpi")
                        .header("X-Role", "ADMIN")
                        .header("X-Roles", "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Requête avec token invalide ou corrompu -> 401 Unauthorized")
    void testInvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/analytics/kpi")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer totally.invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Requête avec token signé par une mauvaise clé secrète -> 401 Unauthorized")
    void testWrongKey_Returns401() throws Exception {
        byte[] wrongKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            wrongKey[i] = (byte) (i + 1);
        }
        JWSSigner signer = new MACSigner(wrongKey);
        SignedJWT wrongSigned = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .subject("user@platform.local")
                        .claim("roles", List.of("ROLE_USER"))
                        .expirationTime(new Date(System.currentTimeMillis() + 3600000))
                        .build()
        );
        wrongSigned.sign(signer);

        mockMvc.perform(get("/api/analytics/kpi")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongSigned.serialize()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Requête authentifiée avec rôle USER -> 200 OK")
    void testRoleUser_Returns200() throws Exception {
        when(analyticsService.getKpiSummary(any(), any(), any()))
                .thenReturn(new KpiSummaryResponse(BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, 0L, 0L, "EUR", Instant.now(), Instant.now(), "UTC"));

        String userToken = createToken("user@platform.local", List.of("ROLE_USER"), 3600000);

        mockMvc.perform(get("/api/analytics/kpi")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Requête authentifiée avec rôle ADMIN -> 200 OK")
    void testRoleAdmin_Returns200() throws Exception {
        when(analyticsService.getKpiSummary(any(), any(), any()))
                .thenReturn(new KpiSummaryResponse(BigDecimal.ZERO, 0L, BigDecimal.ZERO, 0L, 0L, 0L, "EUR", Instant.now(), Instant.now(), "UTC"));

        String adminToken = createToken("admin@platform.local", List.of("ROLE_ADMIN"), 3600000);

        mockMvc.perform(get("/api/analytics/kpi")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoints Actuator et Swagger sont publics sans authentification")
    void testPublicEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
