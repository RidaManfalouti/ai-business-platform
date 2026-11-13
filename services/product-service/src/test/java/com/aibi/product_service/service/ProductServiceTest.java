package com.aibi.product_service.service;

import com.aibi.product_service.dto.ProductRequest;
import com.aibi.product_service.dto.ProductResponse;
import com.aibi.product_service.entity.Product;
import com.aibi.product_service.exception.MissingTenantContextException;
import com.aibi.product_service.exception.ProductAlreadyExistsException;
import com.aibi.product_service.exception.ProductNotFoundException;
import com.aibi.product_service.repository.ProductRepository;
import com.aibi.product_service.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);

        sampleProduct = Product.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .name("Clavier Mécanique")
                .description("Clavier RGB switchs rouges")
                .price(new BigDecimal("89.99"))
                .stockQuantity(15)
                .category("Périphériques")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = new ProductRequest(
                "Clavier Mécanique",
                "Clavier RGB switchs rouges",
                new BigDecimal("89.99"),
                15,
                "Périphériques"
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Création d'un produit avec succès")
    void shouldCreateProductSuccessfully() {
        when(productRepository.existsByTenantIdAndNameIgnoreCase(TENANT_ID, "Clavier Mécanique")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.createProduct(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        assertThat(response.name()).isEqualTo("Clavier Mécanique");
        assertThat(response.price()).isEqualTo(new BigDecimal("89.99"));
        assertThat(response.stockQuantity()).isEqualTo(15);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Échec de création si un produit portant le même nom existe déjà dans le tenant")
    void shouldThrowConflictWhenProductNameExistsOnCreate() {
        when(productRepository.existsByTenantIdAndNameIgnoreCase(TENANT_ID, "Clavier Mécanique")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(sampleRequest))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Échec si contexte tenant absent")
    void shouldThrowMissingTenantContextWhenNoTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> productService.createProduct(sampleRequest))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    @DisplayName("Consultation d'un produit existant par ID")
    void shouldGetProductByIdSuccessfully() {
        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Clavier Mécanique");
    }

    @Test
    @DisplayName("Échec lors de la consultation d'un produit inexistant")
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        when(productRepository.findByTenantIdAndId(TENANT_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Liste paginée des produits par tenant")
    void shouldGetAllProductsWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
        when(productRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

        Page<ProductResponse> result = productService.getAllProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Recherche de produits par nom au sein du tenant")
    void shouldSearchProductsByName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
        when(productRepository.findByTenantIdAndNameContainingIgnoreCase(TENANT_ID, "Clavier", pageable)).thenReturn(page);

        Page<ProductResponse> result = productService.searchProductsByName("Clavier", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Clavier Mécanique");
    }

    @Test
    @DisplayName("Mise à jour d'un produit avec succès")
    void shouldUpdateProductSuccessfully() {
        ProductRequest updateRequest = new ProductRequest(
                "Clavier Mécanique Pro",
                "Version sans fil",
                new BigDecimal("129.99"),
                20,
                "Périphériques"
        );

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(TENANT_ID, "Clavier Mécanique Pro", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("Échec de mise à jour si le nom est déjà utilisé par un autre produit du même tenant")
    void shouldThrowConflictWhenUpdatingWithDuplicateName() {
        ProductRequest updateRequest = new ProductRequest(
                "Souris Gamer",
                "Description",
                new BigDecimal("50.00"),
                10,
                "Périphériques"
        );

        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(TENANT_ID, "Souris Gamer", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(productRepository, never()).save(sampleProduct);
    }

    @Test
    @DisplayName("Suppression d'un produit existant dans le tenant")
    void shouldDeleteProductSuccessfully() {
        when(productRepository.findByTenantIdAndId(TENANT_ID, 1L)).thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct(1L);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("Échec de suppression d'un produit inexistant ou d'un autre tenant")
    void shouldThrowNotFoundWhenDeletingNonExistentProduct() {
        when(productRepository.findByTenantIdAndId(TENANT_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).delete(any());
    }
}
