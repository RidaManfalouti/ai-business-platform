package com.aibi.ai_service.security;

import com.aibi.ai_service.entity.AiConversation;
import com.aibi.ai_service.repository.AiConversationRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiSecurityAndIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiConversationRepository conversationRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        conversationRepository.deleteAll();
    }

    private String createToken(String subject, Long userId, UUID tenantId, List<String> roles) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
            JWSSigner signer = new MACSigner(secretBytes);

            long now = System.currentTimeMillis();
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + 3600000));

            if (userId != null) {
                builder.claim("user_id", userId);
            }
            if (roles != null) {
                builder.claim("roles", roles);
            }
            if (tenantId != null) {
                builder.claim("tenant_id", tenantId.toString());
            }

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Requête non authentifiée sur /api/ai/conversations -> 401 Unauthorized")
    void testUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/ai/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Requête avec token sans claim tenant_id -> 403 Forbidden")
    void testMissingTenantClaim_Returns403() throws Exception {
        String tokenNoTenant = createToken("user@platform.local", 1L, null, List.of("ROLE_USER"));

        mockMvc.perform(get("/api/ai/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenNoTenant))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Revendication de locataire (tenant_id) requise mais absente."));
    }

    @Test
    @DisplayName("Tentative de header spoofing X-Tenant-Id ignorée et rejetée sans JWT valide -> 401")
    void testHeaderSpoofing_IgnoredAndReturns401() throws Exception {
        mockMvc.perform(get("/api/ai/conversations")
                        .header("X-Tenant-Id", TENANT_A.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Endpoints Actuator health et Swagger UI accessibles publiquement -> 200 OK")
    void testActuatorHealthPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Isolation stricte multi-locataires : Tenant B ne peut pas consulter une conversation de Tenant A -> 404")
    void testCrossTenantConversationAccess_Returns404() throws Exception {
        // Conversation créée par Tenant A (User 1)
        AiConversation convA = conversationRepository.save(AiConversation.builder()
                .tenantId(TENANT_A)
                .userId(1L)
                .title("Stratégie Q4 Tenant A")
                .build());

        // Token utilisateur de Tenant B (User 2)
        String tokenTenantB = createToken("tenantB@company.com", 2L, TENANT_B, List.of("ROLE_USER"));

        mockMvc.perform(get("/api/ai/conversations/" + convA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTenantB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Isolation stricte multi-locataires : Tenant B ne peut pas supprimer une conversation de Tenant A -> 404")
    void testCrossTenantConversationDelete_Returns404() throws Exception {
        AiConversation convA = conversationRepository.save(AiConversation.builder()
                .tenantId(TENANT_A)
                .userId(1L)
                .title("Confidentiel Tenant A")
                .build());

        String tokenTenantB = createToken("tenantB@company.com", 2L, TENANT_B, List.of("ROLE_USER"));

        mockMvc.perform(delete("/api/ai/conversations/" + convA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTenantB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Isolation utilisateur intra-tenant : User 2 ne peut pas consulter la conversation privée de User 1 -> 404")
    void testIntraTenantUserIsolation_Returns404() throws Exception {
        AiConversation convUser1 = conversationRepository.save(AiConversation.builder()
                .tenantId(TENANT_A)
                .userId(1L)
                .title("Privé User 1")
                .build());

        String tokenUser2 = createToken("user2@tenanta.com", 2L, TENANT_A, List.of("ROLE_USER"));

        mockMvc.perform(get("/api/ai/conversations/" + convUser1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser2))
                .andExpect(status().isNotFound());
    }
}
