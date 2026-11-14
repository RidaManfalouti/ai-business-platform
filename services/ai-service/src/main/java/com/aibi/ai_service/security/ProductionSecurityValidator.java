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

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
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

        if (isProduction) {
            log.info("Configuration de sécurité validée pour l'environnement PRODUCTION.");
        } else {
            log.info("Environnement DÉVELOPPEMENT détecté — vérifications allégées.");
        }
    }

    public boolean isProductionEnvironment() {
        if ("production".equalsIgnoreCase(appEnv) || "prod".equalsIgnoreCase(appEnv)) {
            return true;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "production".equalsIgnoreCase(p) || "prod".equalsIgnoreCase(p));
    }
}
