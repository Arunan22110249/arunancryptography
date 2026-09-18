package com.orderflow.api.dto;

import com.orderflow.domain.Inventory;
import java.util.UUID;

public record InventoryDto(UUID id, UUID organizationId, UUID productId, int availableQuantity, int reservedQuantity, int soldQuantity, long version) {
    public static InventoryDto fromEntity(Inventory inventory) {
        return new InventoryDto(inventory.getId(), inventory.getOrganizationId(), inventory.getProductId(),
            inventory.getAvailableQuantity(), inventory.getReservedQuantity(), inventory.getSoldQuantity(), inventory.getVersion());
    }
}
