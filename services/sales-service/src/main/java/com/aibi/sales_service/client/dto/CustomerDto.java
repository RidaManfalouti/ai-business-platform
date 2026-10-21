package com.aibi.sales_service.client.dto;

public record CustomerDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String company
) {}
