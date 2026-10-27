package com.aibi.analytics_service.client.dto;

import java.util.List;

public record ProductPageDto(
        List<ProductItemDto> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {}
