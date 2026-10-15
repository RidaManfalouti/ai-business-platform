package com.aibi.product_service.service;

import com.aibi.product_service.dto.ProductRequest;
import com.aibi.product_service.dto.ProductResponse;
import com.aibi.product_service.entity.Product;
import com.aibi.product_service.exception.ProductAlreadyExistsException;
import com.aibi.product_service.exception.ProductNotFoundException;
import com.aibi.product_service.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ProductAlreadyExistsException(
                    "Un produit portant le nom '" + request.name() + "' existe déjà."
            );
        }

        Product product = Product.builder()
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id
                ));
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsByName(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return getAllProducts(pageable);
        }
        return productRepository.findByNameContainingIgnoreCase(name.trim(), pageable)
                .map(ProductResponse::fromEntity);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Produit introuvable avec l'identifiant : " + id
                ));

        String trimmedName = request.name().trim();
        if (productRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
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
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(
                    "Produit introuvable avec l'identifiant : " + id
            );
        }
        productRepository.deleteById(id);
    }
}
