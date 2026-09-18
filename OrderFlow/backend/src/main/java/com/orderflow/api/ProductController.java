package com.orderflow.api;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Duration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderflow.api.dto.ProductDto;
import com.orderflow.domain.Product;
import com.orderflow.repository.ProductRepository;
import com.orderflow.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductRepository productRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ProductController(ProductRepository productRepository, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getProducts(Authentication authentication) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        String cacheKey = "catalog:tenant:" + currentUser.tenantId();
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return ResponseEntity.ok(objectMapper.readValue(cached, new TypeReference<List<ProductDto>>() {}));
            }
        } catch (Exception ignored) {
            // Redis is an acceleration layer; PostgreSQL remains authoritative.
        }
        List<ProductDto> products = productRepository.findByOrganizationId(currentUser.tenantId()).stream()
            .map(ProductDto::fromEntity).collect(Collectors.toList());
        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(products), Duration.ofMinutes(5));
        } catch (Exception ignored) {
            // A cache outage must not fail a catalog read.
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable UUID id, Authentication authentication) {
        CurrentUser.required(authentication);
        CurrentUser currentUser = CurrentUser.required(authentication);
        return productRepository.findByIdAndOrganizationId(id, currentUser.tenantId())
            .map(ProductDto::fromEntity)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto request, Authentication authentication) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        if (!currentUser.canManageCatalog()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setOrganizationId(currentUser.tenantId());
        product.setSku(request.sku());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setActive(request.active());
        product.setCreatedAt(java.time.Instant.now());
        Product saved = productRepository.save(product);
        invalidateCatalog(currentUser.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductDto.fromEntity(saved));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable UUID id, @RequestBody ProductDto request, Authentication authentication) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        if (!currentUser.canManageCatalog()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return productRepository.findByIdAndOrganizationId(id, currentUser.tenantId())
            .map(existing -> {
                existing.setSku(request.sku());
                existing.setName(request.name());
                existing.setCategory(request.category());
                existing.setPrice(request.price());
                existing.setActive(request.active());
                Product saved = productRepository.save(existing);
                invalidateCatalog(currentUser.tenantId());
                return ResponseEntity.ok(ProductDto.fromEntity(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id, Authentication authentication) {
        CurrentUser currentUser = CurrentUser.required(authentication);
        if (!currentUser.canManageCatalog()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!productRepository.existsByIdAndOrganizationId(id, currentUser.tenantId())) {
            return ResponseEntity.notFound().build();
        }
        productRepository.delete(productRepository.findByIdAndOrganizationId(id, currentUser.tenantId()).orElseThrow());
        invalidateCatalog(currentUser.tenantId());
        return ResponseEntity.noContent().build();
    }

    private void invalidateCatalog(UUID tenantId) {
        try {
            redis.delete("catalog:tenant:" + tenantId);
        } catch (RuntimeException ignored) {
            // Cache invalidation is best effort; the TTL bounds stale reads.
        }
    }
}
