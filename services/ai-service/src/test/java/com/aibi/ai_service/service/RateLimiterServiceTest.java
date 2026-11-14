package com.aibi.ai_service.service;

import com.aibi.ai_service.exception.RateLimitExceededException;
import com.aibi.ai_service.provider.LlmProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterServiceTest {

    @Test
    @DisplayName("Requêtes dans la limite autorisée -> ne lève aucune exception")
    void testWithinRateLimit() {
        LlmProperties properties = new LlmProperties();
        properties.getRateLimit().setRequestsPerMinute(5);
        RateLimiterService rateLimiter = new RateLimiterService(properties);

        UUID tenantId = UUID.randomUUID();
        Long userId = 1L;

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> rateLimiter.checkRateLimit(tenantId, userId));
        }
    }

    @Test
    @DisplayName("Requêtes dépassant la limite RPM -> lève RateLimitExceededException")
    void testExceedingRateLimit_ThrowsException() {
        LlmProperties properties = new LlmProperties();
        properties.getRateLimit().setRequestsPerMinute(2);
        RateLimiterService rateLimiter = new RateLimiterService(properties);

        UUID tenantId = UUID.randomUUID();
        Long userId = 1L;

        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(tenantId, userId));
        assertDoesNotThrow(() -> rateLimiter.checkRateLimit(tenantId, userId));

        assertThrows(RateLimitExceededException.class, () ->
                rateLimiter.checkRateLimit(tenantId, userId));
    }
}
