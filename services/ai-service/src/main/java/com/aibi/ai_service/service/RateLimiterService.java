package com.aibi.ai_service.service;

import com.aibi.ai_service.exception.RateLimitExceededException;
import com.aibi.ai_service.provider.LlmProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final LlmProperties properties;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(LlmProperties properties) {
        this.properties = properties;
    }

    public void checkRateLimit(UUID tenantId, Long userId) {
        String key = tenantId.toString() + ":" + userId;
        int maxRpm = properties.getRateLimit().getRequestsPerMinute();

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(maxRpm));
        if (!bucket.tryConsume()) {
            throw new RateLimitExceededException("Limite de requêtes IA atteinte (max " + maxRpm + " req/min). Veuillez patienter quelques instants.");
        }
    }

    private static class TokenBucket {
        private final int capacity;
        private double tokens;
        private long lastRefillTimestamp;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double seconds = (now - lastRefillTimestamp) / 1000.0;
            double refillRatePerSecond = (double) capacity / 60.0;
            tokens = Math.min(capacity, tokens + seconds * refillRatePerSecond);
            lastRefillTimestamp = now;
        }
    }
}
