package com.aibi.ai_service.provider;

import com.aibi.ai_service.exception.LlmServiceException;
import com.aibi.ai_service.security.ProductionSecurityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LlmProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderFactory.class);

    private final LlmProperties properties;
    private final Map<String, LlmProvider> providers;
    private final MockLlmProvider mockLlmProvider;
    private final ProductionSecurityValidator securityValidator;

    public LlmProviderFactory(
            LlmProperties properties,
            List<LlmProvider> providerList,
            MockLlmProvider mockLlmProvider,
            ProductionSecurityValidator securityValidator) {
        this.properties = properties;
        this.mockLlmProvider = mockLlmProvider;
        this.securityValidator = securityValidator;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toLowerCase(), Function.identity()));
    }

    public LlmProvider getProvider() {
        String configuredProvider = properties.getProvider();
        return getProvider(configuredProvider);
    }

    public LlmProvider getProvider(String providerName) {
        boolean isProduction = securityValidator.isProductionEnvironment();

        if (providerName == null || providerName.isBlank()) {
            if (isProduction) {
                log.error("ERREUR CRITIQUE SÉCURITÉ : Aucun fournisseur LLM configuré en production.");
                throw new IllegalStateException("Fournisseur LLM non configuré en production. Le repli vers le mock est interdit.");
            }
            return mockLlmProvider;
        }

        String key = providerName.trim().toLowerCase();

        if (isProduction && "mock".equals(key)) {
            log.error("ERREUR CRITIQUE SÉCURITÉ : Le fournisseur LLM 'mock' est strictement interdit en production.");
            throw new IllegalStateException("Fournisseur LLM 'mock' formellement interdit en environnement de production.");
        }

        LlmProvider provider = providers.get(key);

        if (provider == null) {
            if (isProduction) {
                log.error("ERREUR CRITIQUE SÉCURITÉ : Fournisseur LLM inconnu [{}] en production. Repli interdit.", key);
                throw new IllegalStateException("Fournisseur LLM inconnu [" + key + "] en production. Le repli vers le mock est interdit.");
            }
            log.warn("Fournisseur LLM inconnu [{}], utilisation du fournisseur par défaut [mock].", key);
            return mockLlmProvider;
        }

        if (!provider.isAvailable()) {
            if (isProduction) {
                log.error("ERREUR CRITIQUE SÉCURITÉ : Fournisseur LLM [{}] indisponible en production (clé API absente). Repli interdit.", key);
                throw new IllegalStateException("Fournisseur LLM [" + key + "] non disponible (clé d'API absente ou invalide) en production.");
            }
            if (!"mock".equals(key)) {
                log.warn("Fournisseur LLM [{}] non disponible (clé d'API absente). Bascule automatique sur [mock].", key);
                return mockLlmProvider;
            }
        }

        return provider;
    }
}
