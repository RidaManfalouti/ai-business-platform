package com.aibi.product_service.service;

import com.aibi.product_service.dto.ProductRequest;
import com.aibi.product_service.dto.ProductResponse;
import com.aibi.product_service.entity.Product;
import com.aibi.product_service.exception.ProductAlreadyExistsException;
import com.aibi.product_service.exception.ProductNotFoundException;
import com.aibi.product_service.repository.ProductRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
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

    @Test
    @DisplayName("Création d'un produit avec succès")
    void shouldCreateProductSuccessfully() {
        when(productRepository.existsByNameIgnoreCase("Clavier Mécanique")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.createProduct(sampleRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Clavier Mécanique");
        assertThat(response.price()).isEqualTo(new BigDecimal("89.99"));
        assertThat(response.stockQuantity()).isEqualTo(15);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Échec de création si un produit portant le même nom existe déjà")
    void shouldThrowConflictWhenProductNameExistsOnCreate() {
        when(productRepository.existsByNameIgnoreCase("Clavier Mécanique")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(sampleRequest))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Consultation d'un produit existant par ID")
    void shouldGetProductByIdSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Clavier Mécanique");
    }

    @Test
    @DisplayName("Échec lors de la consultation d'un produit inexistant")
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Liste paginée des produits")
    void shouldGetAllProductsWithPagination() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<ProductResponse> result = productService.getAllProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Recherche de produits par nom")
    void shouldSearchProductsByName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
        when(productRepository.findByNameContainingIgnoreCase("Clavier", pageable)).thenReturn(page);

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

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Clavier Mécanique Pro", 1L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    @DisplayName("Échec de mise à jour si le nom est déjà utilisé par un autre produit")
    void shouldThrowConflictWhenUpdatingWithDuplicateName() {
        ProductRequest updateRequest = new ProductRequest(
                "Souris Gamer",
                "Description",
                new BigDecimal("50.00"),
                10,
                "Périphériques"
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Souris Gamer", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
                .isInstanceOf(ProductAlreadyExistsException.class)
                .hasMessageContaining("existe déjà");

        verify(productRepository, never()).save(sampleProduct);
    }

    @Test
    @DisplayName("Suppression d'un produit existant")
    void shouldDeleteProductSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Échec de suppression d'un produit inexistant")
    void shouldThrowNotFoundWhenDeletingNonExistentProduct() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        verify(productRepository, never()).deleteById(anyLong());
    }
}
