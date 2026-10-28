package com.aibi.analytics_service.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductionSecurityValidatorTest {

    @Test
    @DisplayName("Analytics Service en production avec JWT dev par défaut -> échec au démarrage")
    void shouldFailInProductionWithDefaultJwtSecret() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"production"});

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "SafeProdPassword123!");

        assertThatThrownBy(validator::validateSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La clé par défaut de développement JWT_SECRET ne peut pas être utilisée en production");
    }

    @Test
    @DisplayName("Analytics Service en développement avec secrets par défaut -> avertissement sans échec")
    void shouldAllowDefaultSecretsInDevelopmentMode() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"default"});

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "development");
        ReflectionTestUtils.setField(validator, "jwtSecret", "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "0000");

        assertThatNoException().isThrownBy(validator::validateSecurityConfiguration);
    }
}
