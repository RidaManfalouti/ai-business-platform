package com.aibi.customer_service.service;

import com.aibi.customer_service.dto.CustomerRequest;
import com.aibi.customer_service.dto.CustomerResponse;
import com.aibi.customer_service.entity.Customer;
import com.aibi.customer_service.exception.CustomerAlreadyExistsException;
import com.aibi.customer_service.exception.CustomerNotFoundException;
import com.aibi.customer_service.repository.CustomerRepository;
import com.aibi.customer_service.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String normalizedEmail = request.email().trim().toLowerCase();
        if (customerRepository.existsByTenantIdAndEmailIgnoreCase(tenantId, normalizedEmail)) {
            throw new CustomerAlreadyExistsException(
                    "Un client avec l'adresse email '" + request.email() + "' existe déjà."
            );
        }

        Customer customer = Customer.builder()
                .tenantId(tenantId)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(normalizedEmail)
                .phone(request.phone() != null ? request.phone().trim() : null)
                .company(request.company() != null ? request.company().trim() : null)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(savedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return customerRepository.findByTenantId(tenantId, pageable)
                .map(CustomerResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchByName(String name, Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (name == null || name.isBlank()) {
            return getAllCustomers(pageable);
        }
        return customerRepository.searchByTenantIdAndName(tenantId, name.trim(), pageable)
                .map(CustomerResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchByEmail(String email, Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (email == null || email.isBlank()) {
            return getAllCustomers(pageable);
        }
        return customerRepository.findByTenantIdAndEmailContainingIgnoreCase(tenantId, email.trim(), pageable)
                .map(CustomerResponse::fromEntity);
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));

        String normalizedEmail = request.email().trim().toLowerCase();
        if (customerRepository.existsByTenantIdAndEmailIgnoreCaseAndIdNot(tenantId, normalizedEmail, id)) {
            throw new CustomerAlreadyExistsException(
                    "Un autre client utilise déjà l'adresse email '" + request.email() + "'."
            );
        }

        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhone(request.phone() != null ? request.phone().trim() : null);
        customer.setCompany(request.company() != null ? request.company().trim() : null);

        Customer updatedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(updatedCustomer);
    }

    public void deleteCustomer(Long id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Customer customer = customerRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));
        customerRepository.delete(customer);
    }
}
