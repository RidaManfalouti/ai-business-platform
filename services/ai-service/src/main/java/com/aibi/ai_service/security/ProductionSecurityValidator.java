package com.aibi.ai_service.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ProductionSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    private static final List<String> WEAK_DEV_SECRETS = List.of(
            "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=",
            "default_jwt_secret_key_change_in_production_1234567890",
            "secret"
    );

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${app.env:development}")
    private String appEnv;

    private final Environment environment;
    private final com.aibi.ai_service.provider.LlmProperties llmProperties;

    public ProductionSecurityValidator(
            Environment environment,
            com.aibi.ai_service.provider.LlmProperties llmProperties) {
        this.environment = environment;
        this.llmProperties = llmProperties;
    }

    @PostConstruct
    public void validateSecurityConfiguration() {
        boolean isProduction = isProductionEnvironment();

        if (WEAK_DEV_SECRETS.contains(jwtSecret) || (jwtSecret != null && jwtSecret.length() < 32)) {
            if (isProduction) {
                log.error("ERREUR CRITIQUE SÉCURITÉ : La clé JWT_SECRET par défaut ou trop courte est utilisée en production !");
                throw new IllegalStateException("JWT_SECRET par défaut non autorisé en production");
            } else {
                log.warn("AVERTISSEMENT SÉCURITÉ : Utilisation de la clé de développement JWT_SECRET connue. Cette valeur DOIT être changée en production.");
            }
        }

        if ("0000".equals(dbPassword) || "postgres".equals(dbPassword) || (dbPassword != null && dbPassword.length() < 4)) {
            if (isProduction) {
                log.error("ERREUR CRITIQUE SÉCURITÉ : Mot de passe base de données par défaut utilisé en production !");
                throw new IllegalStateException("POSTGRES_PASSWORD par défaut non autorisé en production");
            } else {
                log.warn("AVERTISSEMENT SÉCURITÉ : La base de données utilise un mot de passe par défaut ou faible. Cette valeur DOIT être changée en production.");
            }
        }

        validateLlmConfiguration(isProduction);

        if (isProduction) {
            log.info("Configuration de sécurité validée pour l'environnement PRODUCTION.");
        } else {
            log.info("Environnement DÉVELOPPEMENT détecté — vérifications allégées.");
        }
    }

    private void validateLlmConfiguration(boolean isProduction) {
        String provider = llmProperties != null ? llmProperties.getProvider() : null;

        if (isProduction) {
            if (provider == null || provider.isBlank()) {
                log.error("ERREUR CRITIQUE SÉCURITÉ : Aucun fournisseur LLM configuré en production (ai.provider)");
                throw new IllegalStateException("Fournisseur LLM requis en production (ai.provider)");
            }

            if ("mock".equalsIgnoreCase(provider.trim())) {
                log.error("ERREUR CRITIQUE SÉCURITÉ : Le fournisseur LLM 'mock' est formellement interdit en environnement de production !");
                throw new IllegalStateException("Fournisseur LLM 'mock' non autorisé en production");
            }

            if ("gemini".equalsIgnoreCase(provider.trim())) {
                String apiKey = llmProperties.getGemini().getApiKey();
                if (apiKey == null || apiKey.isBlank() || isPlaceholderKey(apiKey)) {
                    log.error("ERREUR CRITIQUE SÉCURITÉ : Clé d'API Google Gemini manquante ou invalide pour l'environnement de production !");
                    throw new IllegalStateException("GEMINI_API_KEY requise et valide en production pour le fournisseur gemini");
                }
            } else if ("openai".equalsIgnoreCase(provider.trim())) {
                String apiKey = llmProperties.getOpenai().getApiKey();
                if (apiKey == null || apiKey.isBlank() || isPlaceholderKey(apiKey)) {
                    log.error("ERREUR CRITIQUE SÉCURITÉ : Clé d'API OpenAI manquante ou invalide pour l'environnement de production !");
                    throw new IllegalStateException("OPENAI_API_KEY requise et valide en production pour le fournisseur openai");
                }
            } else {
                log.error("ERREUR CRITIQUE SÉCURITÉ : Fournisseur LLM non reconnu en production : {}", provider);
                throw new IllegalStateException("Fournisseur LLM non reconnu en production: " + provider);
            }
        } else {
            if ("mock".equalsIgnoreCase(provider)) {
                log.info("Mode développement : Fournisseur LLM 'mock' actif.");
            }
        }
    }

    private boolean isPlaceholderKey(String key) {
        String lower = key.toLowerCase();
        return lower.contains("placeholder")
                || lower.contains("change-in-production")
                || lower.startsWith("test-")
                || lower.equals("secret")
                || key.length() < 10;
    }

    public boolean isProductionEnvironment() {
        if ("production".equalsIgnoreCase(appEnv) || "prod".equalsIgnoreCase(appEnv)) {
            return true;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "production".equalsIgnoreCase(p) || "prod".equalsIgnoreCase(p));
    }
}
