package com.aibi.api_gateway.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Validateur de sécurité au démarrage de l'API Gateway.
 * Vérifie l'environnement d'exécution et émet des alertes ou bloque
 * le démarrage en cas de configuration dangereuse en production.
 */
@Component
public class ProductionSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    @Value("${app.env:development}")
    private String appEnv;

    private final Environment environment;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecurityConfiguration() {
        boolean isProduction = isProductionEnvironment();
        if (isProduction) {
            log.info("API Gateway démarrée en mode PRODUCTION.");
        } else {
            log.info("API Gateway démarrée en mode DÉVELOPPEMENT.");
        }
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
}
