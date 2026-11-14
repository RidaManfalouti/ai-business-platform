package com.aibi.ai_service.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
public class LlmProperties {

    private String provider = "mock";
    private GeminiConfig gemini = new GeminiConfig();
    private OpenAiConfig openai = new OpenAiConfig();
    private TimeoutConfig timeout = new TimeoutConfig();
    private RateLimitConfig rateLimit = new RateLimitConfig();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public GeminiConfig getGemini() {
        return gemini;
    }

    public void setGemini(GeminiConfig gemini) {
        this.gemini = gemini;
    }

    public OpenAiConfig getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAiConfig openai) {
        this.openai = openai;
    }

    public TimeoutConfig getTimeout() {
        return timeout;
    }

    public void setTimeout(TimeoutConfig timeout) {
        this.timeout = timeout;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class GeminiConfig {
        private String apiKey = "";
        private String model = "gemini-2.5-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class OpenAiConfig {
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class TimeoutConfig {
        private int connectMs = 5000;
        private int readMs = 30000;

        public int getConnectMs() {
            return connectMs;
        }

        public void setConnectMs(int connectMs) {
            this.connectMs = connectMs;
        }

        public int getReadMs() {
            return readMs;
        }

        public void setReadMs(int readMs) {
            this.readMs = readMs;
        }
    }

    public static class RateLimitConfig {
        private int requestsPerMinute = 30;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }
}
