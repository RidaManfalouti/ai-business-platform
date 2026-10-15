package com.aibi.customer_service.dto;

import com.aibi.customer_service.entity.Customer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Représentation publique d'un client")
public record CustomerResponse(
    @Schema(description = "Identifiant unique du client", example = "1")
    Long id,

    @Schema(description = "Prénom du client", example = "Jean")
    String firstName,

    @Schema(description = "Nom de famille du client", example = "Dupont")
    String lastName,

    @Schema(description = "Adresse email unique", example = "jean.dupont@example.com")
    String email,

    @Schema(description = "Numéro de téléphone", example = "+33612345678")
    String phone,

    @Schema(description = "Nom de l'entreprise", example = "Acme Corp")
    String company,

    @Schema(description = "Date et heure de création", example = "2026-10-15T18:00:00")
    LocalDateTime createdAt,

    @Schema(description = "Date et heure de dernière modification", example = "2026-10-15T18:30:00")
    LocalDateTime updatedAt
) {
    public static CustomerResponse fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
            customer.getId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getCompany(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
