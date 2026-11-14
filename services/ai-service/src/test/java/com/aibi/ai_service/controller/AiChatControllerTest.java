package com.aibi.ai_service.controller;

import com.aibi.ai_service.dto.ChatRequest;
import com.aibi.ai_service.entity.AiConversation;
import com.aibi.ai_service.entity.AiMessage;
import com.aibi.ai_service.entity.MessageRole;
import com.aibi.ai_service.repository.AiConversationRepository;
import com.aibi.ai_service.repository.AiMessageRepository;
import com.aibi.ai_service.tools.client.AnalyticsClient;
import com.aibi.ai_service.tools.client.CustomerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiConversationRepository conversationRepository;

    @Autowired
    private AiMessageRepository messageRepository;

    @MockitoBean
    private AnalyticsClient analyticsClient;

    @MockitoBean
    private CustomerClient customerClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    private String createToken() {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(jwtSecret);
            JWSSigner signer = new MACSigner(secretBytes);

            long now = System.currentTimeMillis();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject("owner@company.local")
                    .claim("user_id", USER_ID)
                    .claim("tenant_id", TENANT_ID.toString())
                    .claim("roles", List.of("ROLE_OWNER", "ROLE_USER"))
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
    @DisplayName("POST /api/ai/chat avec une nouvelle conversation -> crée la conversation et répond")
    void testChatNewConversation_Success() throws Exception {
        ChatRequest request = new ChatRequest(null, "Bonjour, présente-toi");

        mockMvc.perform(post("/api/ai/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId", notNullValue()))
                .andExpect(jsonPath("$.messageId", notNullValue()))
                .andExpect(jsonPath("$.role").value("assistant"))
                .andExpect(jsonPath("$.response", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/ai/chat sur les ventes -> déclenche getSalesSummary et synthétise les données")
    void testChatSalesSummary_TriggersTool() throws Exception {
        when(analyticsClient.getKpiSummary(any(), any(), any()))
                .thenReturn("{\"totalRevenue\":18500.00,\"completedSalesCount\":45,\"averageCartValue\":411.11}");

        ChatRequest request = new ChatRequest(null, "Quel est mon chiffre d'affaires total et mes ventes ?");

        mockMvc.perform(post("/api/ai/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", containsString("18500")));
    }

    @Test
    @DisplayName("POST /api/ai/chat sur les stocks faibles -> déclenche getLowStockProducts")
    void testChatLowStock_TriggersTool() throws Exception {
        when(analyticsClient.getLowStock(anyInt(), anyInt()))
                .thenReturn("{\"lowStockCount\":2,\"items\":[{\"name\":\"Clavier Mécanique\",\"stockQuantity\":2}]}");

        ChatRequest request = new ChatRequest(null, "Quels sont mes produits en rupture de stock ?");

        mockMvc.perform(post("/api/ai/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", containsString("Clavier Mécanique")));
    }

    @Test
    @DisplayName("POST /api/ai/chat poursuite d'une conversation existante -> ajoute les messages à l'historique")
    void testChatExistingConversation_AppendsMessages() throws Exception {
        AiConversation existing = conversationRepository.save(AiConversation.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .title("Discussion existante")
                .build());

        ChatRequest request = new ChatRequest(existing.getId(), "Deuxième question dans ce fil");

        mockMvc.perform(post("/api/ai/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(existing.getId().toString()));

        List<AiMessage> messages = messageRepository.findByConversationIdAndTenantIdOrderByCreatedAtAsc(existing.getId(), TENANT_ID);
        // Expect user message and assistant message
        org.junit.jupiter.api.Assertions.assertEquals(2, messages.size());
        org.junit.jupiter.api.Assertions.assertEquals(MessageRole.USER, messages.get(0).getRole());
        org.junit.jupiter.api.Assertions.assertEquals(MessageRole.ASSISTANT, messages.get(1).getRole());
    }

    @Test
    @DisplayName("POST /api/ai/chat avec message vide -> 400 Bad Request")
    void testChatEmptyMessage_Returns400() throws Exception {
        ChatRequest request = new ChatRequest(null, "   ");

        mockMvc.perform(post("/api/ai/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}
