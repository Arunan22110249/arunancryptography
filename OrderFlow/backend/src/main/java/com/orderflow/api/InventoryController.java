package com.orderflow.api;

import com.orderflow.api.dto.InventoryDto;
import com.orderflow.domain.Inventory;
import com.orderflow.repository.InventoryRepository;
import com.orderflow.repository.ProductRepository;
import com.orderflow.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryController(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public ResponseEntity<List<InventoryDto>> list(Authentication authentication) {
        CurrentUser user = CurrentUser.required(authentication);
        return ResponseEntity.ok(inventoryRepository.findAllByOrganizationId(user.tenantId()).stream().map(InventoryDto::fromEntity).toList());
    }

    @PostMapping
    public ResponseEntity<InventoryDto> upsert(@RequestBody InventoryRequest request, Authentication authentication) {
        CurrentUser user = CurrentUser.required(authentication);
        if (!user.canManageCatalog() || request.quantity() < 0) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (productRepository.findByIdAndOrganizationId(request.productId(), user.tenantId()).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Inventory inventory = inventoryRepository.findByProductIdAndOrganizationId(request.productId(), user.tenantId()).orElseGet(() -> {
            Inventory created = new Inventory();
            created.setId(UUID.randomUUID());
            created.setOrganizationId(user.tenantId());
            created.setProductId(request.productId());
            created.setReservedQuantity(0);
            created.setSoldQuantity(0);
            created.setVersion(0);
            return created;
        });
        inventory.setAvailableQuantity(request.quantity());
        inventory.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(InventoryDto.fromEntity(inventoryRepository.save(inventory)));
    }

    public record InventoryRequest(UUID productId, int quantity) {}
}