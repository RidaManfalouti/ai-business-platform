package com.aibi.product_service.service;

import com.aibi.product_service.dto.ProductRequest;
import com.aibi.product_service.dto.ProductResponse;
import com.aibi.product_service.entity.Product;
import com.aibi.product_service.exception.ProductAlreadyExistsException;
import com.aibi.product_service.exception.ProductNotFoundException;
import com.aibi.product_service.repository.ProductRepository;
import com.aibi.product_service.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (productRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.name().trim())) {
            throw new ProductAlreadyExistsException(
                    "Un produit portant le nom '" + request.name() + "' existe déjà."
            );
        }

        Product product = Product.builder()
                .tenantId(tenantId)
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .category(request.category().trim())
                .build();

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id
                ));
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return productRepository.findByTenantId(tenantId, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsByName(String name, Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        if (name == null || name.isBlank()) {
            return getAllProducts(pageable);
        }
        return productRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, name.trim(), pageable)
                .map(ProductResponse::fromEntity);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id
                ));

        String trimmedName = request.name().trim();
        if (productRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, trimmedName, id)) {
            throw new ProductAlreadyExistsException(
                    "Un autre produit portant le nom '" + trimmedName + "' existe déjà."
            );
        }

        product.setName(trimmedName);
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category().trim());

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(updatedProduct);
    }

    public void deleteProduct(Long id) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        Product product = productRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id
                ));
        productRepository.delete(product);
    }
}
