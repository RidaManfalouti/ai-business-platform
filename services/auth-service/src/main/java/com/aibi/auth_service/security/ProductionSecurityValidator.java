package com.aibi.auth_service.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Set;

/**
 * Validateur de configuration de sécurité au démarrage.
 * Empêche le démarrage du microservice en production si des secrets par défaut
 * ou des mots de passe manifestement faibles sont détectés (Fail-Fast).
 */
@Component
public class ProductionSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    private static final String DEFAULT_DEV_JWT_SECRET = "dGhpc2lzYXZlcnlzZWNyZXRrZXlmb3Jqd3RoZXJlMTIzNDU=";
    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "0000", "postgres", "password", "admin", "root", "123456", "secret"
    );

    @Value("${app.env:development}")
    private String appEnv;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    private final Environment environment;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecurityConfiguration() {
        boolean isProduction = isProductionEnvironment();

        validateJwtSecret(isProduction);
        validateDatasourcePassword(isProduction);
    }

    public boolean isProductionEnvironment() {
        if ("production".equalsIgnoreCase(appEnv) || "prod".equalsIgnoreCase(appEnv)) {
            return true;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("production".equalsIgnoreCase(profile) || "prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private void validateJwtSecret(boolean isProduction) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            if (isProduction) {
                throw new IllegalStateException("ERREUR CRITIQUE DE SÉCURITÉ : JWT_SECRET doit obligatoirement être défini en environnement de production.");
            } else {
                log.warn("AVERTISSEMENT SÉCURITÉ : Aucun JWT_SECRET configuré.");
                return;
            }
        }

        boolean isDefaultSecret = DEFAULT_DEV_JWT_SECRET.equals(jwtSecret.trim());
        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(jwtSecret.trim());
        } catch (IllegalArgumentException ex) {
            if (isProduction) {
                throw new IllegalStateException("ERREUR CRITIQUE DE SÉCURITÉ : JWT_SECRET n'est pas une clé Base64 valide.", ex);
            } else {
                log.warn("AVERTISSEMENT SÉCURITÉ : JWT_SECRET n'est pas une clé Base64 valide.");
                return;
            }
        }

        boolean isKeyTooShort = decodedKey.length < 32;

        if (isProduction) {
            if (isDefaultSecret) {
                throw new IllegalStateException("ERREUR CRITIQUE DE SÉCURITÉ : La clé par défaut de développement JWT_SECRET ne peut pas être utilisée en production.");
            }
            if (isKeyTooShort) {
                throw new IllegalStateException("ERREUR CRITIQUE DE SÉCURITÉ : JWT_SECRET doit comporter au moins 256 bits (32 octets). Longueur actuelle : " + decodedKey.length);
            }
        } else {
            if (isDefaultSecret) {
                log.warn("AVERTISSEMENT SÉCURITÉ : Utilisation de la clé de développement JWT_SECRET connue. Cette valeur DOIT être changée en production.");
            }
            if (isKeyTooShort) {
                log.warn("AVERTISSEMENT SÉCURITÉ : La clé JWT_SECRET comporte moins de 256 bits.");
            }
        }
    }

    private void validateDatasourcePassword(boolean isProduction) {
        String trimmedPassword = (datasourcePassword != null) ? datasourcePassword.trim() : "";
        boolean isWeak = trimmedPassword.isEmpty() || WEAK_PASSWORDS.contains(trimmedPassword.toLowerCase());

        if (isProduction && isWeak) {
            throw new IllegalStateException("ERREUR CRITIQUE DE SÉCURITÉ : Un mot de passe de base de données faible ou par défaut a été détecté en production.");
        } else if (isWeak) {
            log.warn("AVERTISSEMENT SÉCURITÉ : La base de données utilise un mot de passe par défaut ou faible. Cette valeur DOIT être changée en production.");
        }
    }
}
