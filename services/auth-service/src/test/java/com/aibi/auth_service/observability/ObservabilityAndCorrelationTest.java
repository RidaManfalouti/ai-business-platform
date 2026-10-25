package com.aibi.auth_service.observability;

import com.aibi.auth_service.filter.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ObservabilityAndCorrelationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F-]{36}$");

    @Test
    @DisplayName("Génère un UUID de corrélation si l'en-tête est manquant")
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String header = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
                    assertThat(header).isNotNull();
                    assertThat(UUID_PATTERN.matcher(header).matches()).isTrue();
                });
    }

    @Test
    @DisplayName("Conserve un identifiant de corrélation valide")
    void shouldPreserveValidCorrelationId() throws Exception {
        String validId = "custom-auth-trace-9999";
        mockMvc.perform(get("/actuator/health")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, validId))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, validId));
    }

    @Test
    @DisplayName("Expose publiquement /actuator/prometheus sans nécessiter d'authentification")
    void shouldAllowPublicAccessToPrometheus() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protège ou refuse l'accès aux endpoints non exposés comme /actuator/env")
    void shouldDenyAccessToSensitiveEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Soit 401/403 (non autorisé) soit 404 (non exposé dans include)
                    assertThat(status).isIn(401, 403, 404);
                });
    }
}
