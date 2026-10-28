package com.aibi.auth_service.security;

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
    @DisplayName("Mode production avec clé JWT de dev par défaut -> échec immédiat au démarrage (IllegalStateException)")
    void shouldFailInProductionWithDefaultJwtSecret() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"production"});

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "RealStrongPassword123!");

        assertThatThrownBy(validator::validateSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La clé par défaut de développement JWT_SECRET ne peut pas être utilisée en production");
    }

    @Test
    @DisplayName("Mode production avec mot de passe postgres faible ('0000') -> échec immédiat au démarrage")
    void shouldFailInProductionWithWeakDatabasePassword() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"production"});

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        // A valid strong random 256-bit base64 key
        ReflectionTestUtils.setField(validator, "jwtSecret", "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "0000");

        assertThatThrownBy(validator::validateSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Un mot de passe de base de données faible ou par défaut a été détecté en production");
    }

    @Test
    @DisplayName("Mode développement avec secrets par défaut -> avertissement sans échec")
    void shouldAllowDefaultSecretsInDevelopmentMode() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"default"});

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "development");
        ReflectionTestUtils.setField(validator, "jwtSecret", "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "0000");

        assertThatNoException().isThrownBy(validator::validateSecurityConfiguration);
    }

    @Test
    @DisplayName("Mode production avec secrets forts et valides -> validation réussie")
    void shouldPassInProductionWithStrongSecrets() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"production"});

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "SuperSecureDbPass2026!#");

        assertThatNoException().isThrownBy(validator::validateSecurityConfiguration);
    }
}
