package com.aibi.customer_service.service;

import com.aibi.customer_service.dto.CustomerRequest;
import com.aibi.customer_service.dto.CustomerResponse;
import com.aibi.customer_service.entity.Customer;
import com.aibi.customer_service.exception.CustomerAlreadyExistsException;
import com.aibi.customer_service.exception.CustomerNotFoundException;
import com.aibi.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;
    private CustomerRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleCustomer = Customer.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Martin")
                .email("alice.martin@example.com")
                .phone("+33612345678")
                .company("Acme Corp")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = new CustomerRequest(
                "Alice",
                "Martin",
                "alice.martin@example.com",
                "+33612345678",
                "Acme Corp"
        );
    }

    @Test
    @DisplayName("Création d'un client avec succès")
    void shouldCreateCustomerSuccessfully() {
        when(customerRepository.existsByEmailIgnoreCase("alice.martin@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponse response = customerService.createCustomer(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Martin");
        assertThat(response.email()).isEqualTo("alice.martin@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Échec de création si l'email existe déjà (409)")
    void shouldThrowConflictWhenEmailAlreadyExistsOnCreate() {
        when(customerRepository.existsByEmailIgnoreCase("alice.martin@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(sampleRequest))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Consultation d'un client par identifiant")
    void shouldGetCustomerByIdSuccessfully() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        CustomerResponse response = customerService.getCustomerById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice.martin@example.com");
    }

    @Test
    @DisplayName("Échec lors de la consultation d'un client inexistant (404)")
    void shouldThrowNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(999L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("Liste paginée des clients")
    void shouldGetAllCustomersWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(sampleCustomer), pageable, 1);
        when(customerRepository.findAll(pageable)).thenReturn(page);

        Page<CustomerResponse> result = customerService.getAllCustomers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Recherche de clients par nom ou prénom")
    void shouldSearchCustomersByName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(sampleCustomer), pageable, 1);
        when(customerRepository.searchByName("Martin", pageable)).thenReturn(page);

        Page<CustomerResponse> result = customerService.searchByName("Martin", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).lastName()).isEqualTo("Martin");
    }

    @Test
    @DisplayName("Recherche de clients par email")
    void shouldSearchCustomersByEmail() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Customer> page = new PageImpl<>(List.of(sampleCustomer), pageable, 1);
        when(customerRepository.findByEmailContainingIgnoreCase("alice", pageable)).thenReturn(page);

        Page<CustomerResponse> result = customerService.searchByEmail("alice", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("alice.martin@example.com");
    }

    @Test
    @DisplayName("Mise à jour d'un client avec succès")
    void shouldUpdateCustomerSuccessfully() {
        CustomerRequest updateRequest = new CustomerRequest(
                "Alice",
                "Martin-Dupont",
                "alice.new@example.com",
                "+33699999999",
                "New Company"
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("alice.new@example.com", 1L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponse response = customerService.updateCustomer(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(customerRepository).save(sampleCustomer);
    }

    @Test
    @DisplayName("Échec de mise à jour si l'email est déjà utilisé par un autre client (409)")
    void shouldThrowConflictWhenUpdatingWithDuplicateEmail() {
        CustomerRequest updateRequest = new CustomerRequest(
                "Alice",
                "Martin",
                "bob@example.com",
                "+33612345678",
                "Acme"
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("bob@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.updateCustomer(1L, updateRequest))
                .isInstanceOf(CustomerAlreadyExistsException.class)
                .hasMessageContaining("utilise déjà");

        verify(customerRepository, never()).save(sampleCustomer);
    }

    @Test
    @DisplayName("Suppression d'un client existant (204)")
    void shouldDeleteCustomerSuccessfully() {
        when(customerRepository.existsById(1L)).thenReturn(true);

        customerService.deleteCustomer(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Échec de suppression d'un client inexistant (404)")
    void shouldThrowNotFoundWhenDeletingNonExistentCustomer() {
        when(customerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.deleteCustomer(999L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("999");

        verify(customerRepository, never()).deleteById(anyLong());
    }
}
