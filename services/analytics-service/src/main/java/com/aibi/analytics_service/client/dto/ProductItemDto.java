package com.aibi.analytics_service.client.dto;

import java.math.BigDecimal;

public record ProductItemDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String category
) {}
