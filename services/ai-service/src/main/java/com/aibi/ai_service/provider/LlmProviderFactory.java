package com.aibi.ai_service.provider;

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

    public LlmProviderFactory(LlmProperties properties, List<LlmProvider> providerList, MockLlmProvider mockLlmProvider) {
        this.properties = properties;
        this.mockLlmProvider = mockLlmProvider;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toLowerCase(), Function.identity()));
    }

    public LlmProvider getProvider() {
        String configuredProvider = properties.getProvider();
        return getProvider(configuredProvider);
    }

    public LlmProvider getProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return mockLlmProvider;
        }

        String key = providerName.trim().toLowerCase();
        LlmProvider provider = providers.get(key);

        if (provider == null) {
            log.warn("Fournisseur LLM inconnu [{}], utilisation du fournisseur par défaut [mock].", key);
            return mockLlmProvider;
        }

        if (!provider.isAvailable() && !"mock".equals(key)) {
            log.warn("Fournisseur LLM [{}] non disponible (clé d'API absente). Bascule automatique sur [mock].", key);
            return mockLlmProvider;
        }

        return provider;
    }
}
