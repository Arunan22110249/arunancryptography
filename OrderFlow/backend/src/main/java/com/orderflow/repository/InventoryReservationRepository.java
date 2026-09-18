package com.orderflow.repository;

import com.orderflow.domain.InventoryReservation;
import com.orderflow.domain.InventoryReservationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    List<InventoryReservation> findAllByOrderIdAndOrganizationId(UUID orderId, UUID organizationId);
    Optional<InventoryReservation> findByOrderIdAndProductIdAndOrganizationId(UUID orderId, UUID productId, UUID organizationId);
    List<InventoryReservation> findAllByOrderIdAndOrganizationIdAndStatus(UUID orderId, UUID organizationId, InventoryReservationStatus status);
}
