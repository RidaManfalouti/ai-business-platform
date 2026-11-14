package com.aibi.ai_service.tools;

import com.aibi.ai_service.exception.InvalidToolArgumentException;
import com.aibi.ai_service.tools.client.AnalyticsClient;
import com.aibi.ai_service.tools.client.CustomerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class BusinessToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(BusinessToolExecutor.class);
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final int MAX_RESPONSE_LENGTH = 16384;

    private final AnalyticsClient analyticsClient;
    private final CustomerClient customerClient;
    private final ObjectMapper objectMapper;

    public BusinessToolExecutor(AnalyticsClient analyticsClient, CustomerClient customerClient, ObjectMapper objectMapper) {
        this.analyticsClient = analyticsClient;
        this.customerClient = customerClient;
        this.objectMapper = objectMapper;
    }

    public String executeTool(String toolName, String argumentsJson) {
        if (toolName == null || toolName.isBlank()) {
            throw new InvalidToolArgumentException("Nom d'outil vide ou manquant.");
        }

        JsonNode args;
        try {
            args = (argumentsJson == null || argumentsJson.isBlank())
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(argumentsJson);
        } catch (Exception e) {
            throw new InvalidToolArgumentException("Format JSON des arguments d'outil invalide: " + e.getMessage());
        }

        String result;
        switch (toolName) {
            case "getSalesSummary" -> result = handleGetSalesSummary(args);
            case "getSalesByPeriod" -> result = handleGetSalesByPeriod(args);
            case "getTopSellingProducts" -> result = handleGetTopSellingProducts(args);
            case "getLowStockProducts" -> result = handleGetLowStockProducts(args);
            case "getTopCustomers" -> result = handleGetTopCustomers(args);
            case "compareRevenuePeriods" -> result = handleCompareRevenuePeriods(args);
            default -> throw new InvalidToolArgumentException("Outil inconnu ou non supporté: " + toolName);
        }

        return truncateResponse(result);
    }

    private String handleGetSalesSummary(JsonNode args) {
        String from = getOptionalDate(args, "from");
        String to = getOptionalDate(args, "to");
        String timezone = getOptionalString(args, "timezone");
        return analyticsClient.getKpiSummary(from, to, timezone);
    }

    private String handleGetSalesByPeriod(JsonNode args) {
        String from = getOptionalDate(args, "from");
        String to = getOptionalDate(args, "to");
        String timezone = getOptionalString(args, "timezone");
        return analyticsClient.getTrends(from, to, timezone);
    }

    private String handleGetTopSellingProducts(JsonNode args) {
        String from = getOptionalDate(args, "from");
        String to = getOptionalDate(args, "to");
        String timezone = getOptionalString(args, "timezone");
        int limit = getBoundedInt(args, "limit", 5, 1, 20);
        return analyticsClient.getTopProducts(from, to, timezone, limit);
    }

    private String handleGetLowStockProducts(JsonNode args) {
        int threshold = getBoundedInt(args, "threshold", 10, 0, 1000);
        int limit = getBoundedInt(args, "limit", 10, 1, 50);
        return analyticsClient.getLowStock(threshold, limit);
    }

    private String handleGetTopCustomers(JsonNode args) {
        int limit = getBoundedInt(args, "limit", 10, 1, 25);
        return customerClient.getCustomers(limit);
    }

    private String handleCompareRevenuePeriods(JsonNode args) {
        String p1From = getRequiredDate(args, "period1From");
        String p1To = getRequiredDate(args, "period1To");
        String p2From = getRequiredDate(args, "period2From");
        String p2To = getRequiredDate(args, "period2To");

        String p1Summary = analyticsClient.getKpiSummary(p1From, p1To, "UTC");
        String p2Summary = analyticsClient.getKpiSummary(p2From, p2To, "UTC");

        return String.format("{\"period1\":%s,\"period2\":%s}", p1Summary, p2Summary);
    }

    private String getOptionalDate(JsonNode args, String field) {
        if (args.has(field) && !args.get(field).isNull()) {
            String val = args.get(field).asText().trim();
            if (!val.isEmpty()) {
                if (!DATE_PATTERN.matcher(val).matches()) {
                    throw new InvalidToolArgumentException("Format de date invalide pour '" + field + "'. Attendu : YYYY-MM-DD");
                }
                return val;
            }
        }
        return null;
    }

    private String getRequiredDate(JsonNode args, String field) {
        String date = getOptionalDate(args, field);
        if (date == null) {
            throw new InvalidToolArgumentException("Le paramètre '" + field + "' est requis au format YYYY-MM-DD.");
        }
        return date;
    }

    private String getOptionalString(JsonNode args, String field) {
        if (args.has(field) && !args.get(field).isNull()) {
            String val = args.get(field).asText().trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }

    private int getBoundedInt(JsonNode args, String field, int defaultValue, int min, int max) {
        if (args.has(field) && !args.get(field).isNull()) {
            int val = args.get(field).asInt(defaultValue);
            if (val < min || val > max) {
                throw new InvalidToolArgumentException(String.format("Valeur hors limites pour '%s': %d (min: %d, max: %d)", field, val, min, max));
            }
            return val;
        }
        return defaultValue;
    }

    private String truncateResponse(String response) {
        if (response != null && response.length() > MAX_RESPONSE_LENGTH) {
            log.warn("Troncature de la réponse d'outil (taille: {} > max: {})", response.length(), MAX_RESPONSE_LENGTH);
            return response.substring(0, MAX_RESPONSE_LENGTH) + "... [Données tronquées pour respecter les limites]";
        }
        return response;
    }
}
