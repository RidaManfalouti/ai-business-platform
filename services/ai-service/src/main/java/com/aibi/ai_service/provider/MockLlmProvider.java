package com.aibi.ai_service.provider;

import com.aibi.ai_service.provider.dto.LlmMessage;
import com.aibi.ai_service.provider.dto.LlmRequest;
import com.aibi.ai_service.provider.dto.LlmResponse;
import com.aibi.ai_service.provider.dto.LlmToolCall;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("mockLlmProvider")
public class MockLlmProvider implements LlmProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LlmResponse generateResponse(LlmRequest request) {
        List<LlmMessage> messages = request.messages();
        if (messages.isEmpty()) {
            return LlmResponse.ofText("Bonjour ! Comment puis-je vous aider aujourd'hui avec votre entreprise ?", getProviderName(), "mock-v1");
        }

        // If the last message is a TOOL result, generate a comprehensive business summary
        LlmMessage lastMessage = messages.get(messages.size() - 1);
        if ("tool".equalsIgnoreCase(lastMessage.role())) {
            return LlmResponse.ofText(
                    "Voici l'analyse des données de votre entreprise :\n\n"
                    + lastMessage.content()
                    + "\n\nN'hésitez pas à me demander des recommandations complémentaires !",
                    getProviderName(),
                    "mock-v1"
            );
        }

        // Analyze user prompt to check if a business tool should be triggered
        String userQuery = lastMessage.content().toLowerCase();

        if (hasTool(request, "getSalesSummary") && (userQuery.contains("vente") || userQuery.contains("sale") || userQuery.contains("chiffre") || userQuery.contains("revenue") || userQuery.contains("ca"))) {
            return LlmResponse.ofToolCalls(
                    List.of(new LlmToolCall("call_" + UUID.randomUUID().toString().substring(0, 8), "getSalesSummary", "{}")),
                    getProviderName(),
                    "mock-v1"
            );
        }

        if (hasTool(request, "getLowStockProducts") && (userQuery.contains("stock") || userQuery.contains("inventaire") || userQuery.contains("rupture"))) {
            return LlmResponse.ofToolCalls(
                    List.of(new LlmToolCall("call_" + UUID.randomUUID().toString().substring(0, 8), "getLowStockProducts", "{\"threshold\":10}")),
                    getProviderName(),
                    "mock-v1"
            );
        }

        if (hasTool(request, "getTopSellingProducts") && (userQuery.contains("top") || userQuery.contains("meilleur") || userQuery.contains("populaire") || userQuery.contains("best-selling"))) {
            return LlmResponse.ofToolCalls(
                    List.of(new LlmToolCall("call_" + UUID.randomUUID().toString().substring(0, 8), "getTopSellingProducts", "{\"limit\":5}")),
                    getProviderName(),
                    "mock-v1"
            );
        }

        if (hasTool(request, "getTopCustomers") && (userQuery.contains("client") || userQuery.contains("customer"))) {
            return LlmResponse.ofToolCalls(
                    List.of(new LlmToolCall("call_" + UUID.randomUUID().toString().substring(0, 8), "getTopCustomers", "{\"limit\":10}")),
                    getProviderName(),
                    "mock-v1"
            );
        }

        // Generic intelligent fallback answer
        return LlmResponse.ofText(
                "Je suis votre assistant commercial d'entreprise. Vous pouvez me poser des questions sur vos ventes, votre chiffre d'affaires, vos stocks faibles, vos meilleurs produits ou vos clients.",
                getProviderName(),
                "mock-v1"
        );
    }

    private boolean hasTool(LlmRequest request, String toolName) {
        if (request.tools() == null) {
            return false;
        }
        return request.tools().stream().anyMatch(t -> t.name().equalsIgnoreCase(toolName));
    }
}
