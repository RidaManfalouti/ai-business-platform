package com.aibi.api_gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ProductionSecurityValidatorTest {

    @Test
    @DisplayName("Devrait accepter le démarrage en mode développement")
    void shouldAcceptDevelopmentEnvironment() {
        MockEnvironment env = new MockEnvironment();
        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "development");

        assertThatCode(validator::validateSecurityConfiguration).doesNotThrowAnyException();
        assertThat(validator.isProductionEnvironment()).isFalse();
    }

    @Test
    @DisplayName("Devrait détecter le profil prod ou production")
    void shouldDetectProductionEnvironment() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("production");
        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "development");

        assertThat(validator.isProductionEnvironment()).isTrue();
    }

    @Test
    @DisplayName("Devrait détecter la variable APP_ENV=production")
    void shouldDetectAppEnvProduction() {
        MockEnvironment env = new MockEnvironment();
        ProductionSecurityValidator validator = new ProductionSecurityValidator(env);
        ReflectionTestUtils.setField(validator, "appEnv", "production");

        assertThat(validator.isProductionEnvironment()).isTrue();
    }
}
