package com.aibi.analytics_service.client.dto;

import java.util.List;

public record CustomerPageDto(
        List<Object> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {}
