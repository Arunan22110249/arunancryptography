package com.orderflow.api.dto;

import com.orderflow.domain.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
    UUID id,
    UUID organizationId,
    String sku,
    String name,
    String category,
    BigDecimal price,
    boolean active
) {
    public static ProductDto fromEntity(Product product) {
        return new ProductDto(
            product.getId(),
            product.getOrganizationId(),
            product.getSku(),
            product.getName(),
            product.getCategory(),
            product.getPrice(),
            product.isActive()
        );
    }
}
