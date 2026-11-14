package com.aibi.ai_service.provider;

import com.aibi.ai_service.provider.dto.LlmRequest;
import com.aibi.ai_service.provider.dto.LlmResponse;

public interface LlmProvider {

    String getProviderName();

    LlmResponse generateResponse(LlmRequest request);

    boolean isAvailable();
}
