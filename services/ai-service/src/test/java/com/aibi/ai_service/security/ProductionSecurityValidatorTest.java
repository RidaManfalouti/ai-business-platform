package com.aibi.ai_service.security;

import com.aibi.ai_service.provider.LlmProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProductionSecurityValidatorTest {

    @Test
    @DisplayName("CRIT-06 : En production avec ai.provider=mock -> lève IllegalStateException")
    void testProductionRejectsMockProvider() {
        StandardEnvironment env = new StandardEnvironment();
        LlmProperties llmProps = new LlmProperties();
        llmProps.setProvider("mock");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, llmProps);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "aVeryStrongSecureProductionSecretKeyWithMoreThan32Chars!");
        ReflectionTestUtils.setField(validator, "dbPassword", "StrongDbPassword123!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfiguration);
        assertTrue(ex.getMessage().contains("mock"));
    }

    @Test
    @DisplayName("CRIT-06 : En production avec provider gemini sans clé API valide -> lève IllegalStateException")
    void testProductionRejectsGeminiWithoutValidApiKey() {
        StandardEnvironment env = new StandardEnvironment();
        LlmProperties llmProps = new LlmProperties();
        llmProps.setProvider("gemini");
        llmProps.getGemini().setApiKey(""); // Clé vide

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, llmProps);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "aVeryStrongSecureProductionSecretKeyWithMoreThan32Chars!");
        ReflectionTestUtils.setField(validator, "dbPassword", "StrongDbPassword123!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfiguration);
        assertTrue(ex.getMessage().contains("GEMINI_API_KEY"));
    }

    @Test
    @DisplayName("CRIT-06 : En production avec provider gemini et placeholder key -> lève IllegalStateException")
    void testProductionRejectsGeminiWithPlaceholderKey() {
        StandardEnvironment env = new StandardEnvironment();
        LlmProperties llmProps = new LlmProperties();
        llmProps.setProvider("gemini");
        llmProps.getGemini().setApiKey("change-in-production-placeholder");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, llmProps);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "aVeryStrongSecureProductionSecretKeyWithMoreThan32Chars!");
        ReflectionTestUtils.setField(validator, "dbPassword", "StrongDbPassword123!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfiguration);
        assertTrue(ex.getMessage().contains("GEMINI_API_KEY"));
    }

    @Test
    @DisplayName("CRIT-06 : En production avec provider openai sans clé API -> lève IllegalStateException")
    void testProductionRejectsOpenAiWithoutKey() {
        StandardEnvironment env = new StandardEnvironment();
        LlmProperties llmProps = new LlmProperties();
        llmProps.setProvider("openai");
        llmProps.getOpenai().setApiKey("");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, llmProps);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "aVeryStrongSecureProductionSecretKeyWithMoreThan32Chars!");
        ReflectionTestUtils.setField(validator, "dbPassword", "StrongDbPassword123!");

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateSecurityConfiguration);
        assertTrue(ex.getMessage().contains("OPENAI_API_KEY"));
    }

    @Test
    @DisplayName("CRIT-06 : En développement avec mock provider -> validation acceptée")
    void testDevelopmentAllowsMockProvider() {
        StandardEnvironment env = new StandardEnvironment();
        LlmProperties llmProps = new LlmProperties();
        llmProps.setProvider("mock");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, llmProps);
        ReflectionTestUtils.setField(validator, "appEnv", "development");
        ReflectionTestUtils.setField(validator, "jwtSecret", "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=");
        ReflectionTestUtils.setField(validator, "dbPassword", "0000");

        assertDoesNotThrow(validator::validateSecurityConfiguration);
    }

    @Test
    @DisplayName("CRIT-06 : En production avec configuration complète valide -> validation acceptée")
    void testProductionWithValidConfig_Succeeds() {
        StandardEnvironment env = new StandardEnvironment();
        LlmProperties llmProps = new LlmProperties();
        llmProps.setProvider("gemini");
        llmProps.getGemini().setApiKey("AIzaSyValidProductionSecretKeySample123456");

        ProductionSecurityValidator validator = new ProductionSecurityValidator(env, llmProps);
        ReflectionTestUtils.setField(validator, "appEnv", "production");
        ReflectionTestUtils.setField(validator, "jwtSecret", "aVeryStrongSecureProductionSecretKeyWithMoreThan32Chars!");
        ReflectionTestUtils.setField(validator, "dbPassword", "StrongDbPassword123!");

        assertDoesNotThrow(validator::validateSecurityConfiguration);
    }
}
