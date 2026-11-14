package com.aibi.ai_service.controller;

import com.aibi.ai_service.entity.AiConversation;
import com.aibi.ai_service.entity.AiMessage;
import com.aibi.ai_service.entity.MessageRole;
import com.aibi.ai_service.repository.AiConversationRepository;
import com.aibi.ai_service.repository.AiMessageRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AiConversationRepository conversationRepository;

    @Autowired
    private AiMessageRepository messageRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long USER_ID = 50L;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    private String createToken(Long userId, UUID tenantId) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
            JWSSigner signer = new MACSigner(secretBytes);

            long now = System.currentTimeMillis();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject("user@business.local")
                    .claim("user_id", userId)
                    .claim("tenant_id", tenantId.toString())
                    .claim("roles", List.of("ROLE_USER"))
                    .issueTime(new Date(now))
                    .expirationTime(new Date(now + 3600000))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("GET /api/ai/conversations -> retourne les conversations de l'utilisateur")
    void testGetConversations_Success() throws Exception {
        conversationRepository.save(AiConversation.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .title("Analyse Ventes Octobre")
                .build());

        mockMvc.perform(get("/api/ai/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(USER_ID, TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Analyse Ventes Octobre"));
    }

    @Test
    @DisplayName("GET /api/ai/conversations/{id} -> retourne la conversation avec ses messages")
    void testGetConversationDetail_Success() throws Exception {
        AiConversation conv = conversationRepository.save(AiConversation.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .title("Analyse Stock")
                .build());

        messageRepository.save(AiMessage.builder()
                .conversation(conv)
                .tenantId(TENANT_ID)
                .role(MessageRole.USER)
                .content("Produits faibles ?")
                .build());

        messageRepository.save(AiMessage.builder()
                .conversation(conv)
                .tenantId(TENANT_ID)
                .role(MessageRole.ASSISTANT)
                .content("Voici vos 2 produits avec stock faible...")
                .build());

        mockMvc.perform(get("/api/ai/conversations/" + conv.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(USER_ID, TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conv.getId().toString()))
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"));
    }

    @Test
    @DisplayName("DELETE /api/ai/conversations/{id} -> supprime la conversation et ses messages en cascade")
    void testDeleteConversation_Success() throws Exception {
        AiConversation conv = conversationRepository.save(AiConversation.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .title("À supprimer")
                .build());

        mockMvc.perform(delete("/api/ai/conversations/" + conv.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken(USER_ID, TENANT_ID)))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(conversationRepository.existsById(conv.getId()));
    }
}
