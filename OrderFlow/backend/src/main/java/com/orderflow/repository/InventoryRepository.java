package com.orderflow.repository;

import com.orderflow.domain.Inventory;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findAllByOrganizationId(UUID organizationId);
    Optional<Inventory> findByProductIdAndOrganizationId(UUID productId, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productId = :productId and i.organizationId = :organizationId")
    Optional<Inventory> lockByProductIdAndOrganizationId(UUID productId, UUID organizationId);
}
