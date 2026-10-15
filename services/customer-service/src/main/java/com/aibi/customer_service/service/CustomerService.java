package com.aibi.customer_service.service;

import com.aibi.customer_service.dto.CustomerRequest;
import com.aibi.customer_service.dto.CustomerResponse;
import com.aibi.customer_service.entity.Customer;
import com.aibi.customer_service.exception.CustomerAlreadyExistsException;
import com.aibi.customer_service.exception.CustomerNotFoundException;
import com.aibi.customer_service.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new CustomerAlreadyExistsException(
                    "Un client avec l'adresse email '" + request.email() + "' existe déjà."
            );
        }

        Customer customer = Customer.builder()
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
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(CustomerResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchByName(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return getAllCustomers(pageable);
        }
        return customerRepository.searchByName(name.trim(), pageable)
                .map(CustomerResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> searchByEmail(String email, Pageable pageable) {
        if (email == null || email.isBlank()) {
            return getAllCustomers(pageable);
        }
        return customerRepository.findByEmailContainingIgnoreCase(email.trim(), pageable)
                .map(CustomerResponse::fromEntity);
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Client introuvable avec l'identifiant : " + id
                ));

        String normalizedEmail = request.email().trim().toLowerCase();
        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
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
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException(
                    "Client introuvable avec l'identifiant : " + id
            );
        }
        customerRepository.deleteById(id);
    }
}
