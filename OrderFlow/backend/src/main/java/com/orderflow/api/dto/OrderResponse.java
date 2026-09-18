package com.orderflow.api.dto;

import com.orderflow.domain.Order;
import com.orderflow.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID organizationId,
    UUID customerId,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency,
    Instant createdAt,
    Instant updatedAt,
    long version
) {
    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getOrganizationId(),
            order.getCustomerId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getVersion()
        );
    }
}
