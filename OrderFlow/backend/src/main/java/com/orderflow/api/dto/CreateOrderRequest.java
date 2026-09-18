package com.orderflow.api.dto;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(List<Item> items) {
    public record Item(UUID productId, int quantity) {}
}
