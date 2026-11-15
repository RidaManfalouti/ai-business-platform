package com.aibi.analytics_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO allégé pour la récupération du nom commercial du client.
 * Applique le principe de minimisation des données (Data Minimization) :
 * Les champs PII tels que email, phone, address sont strictement exclus.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerSummaryDto(
        Long id,
        String firstName,
        String lastName,
        String company
) {
    public String displayName() {
        if (company != null && !company.isBlank()) {
            return company.trim();
        }
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String full = (first + " " + last).trim();
        return !full.isEmpty() ? full : "Client #" + id;
    }
}
