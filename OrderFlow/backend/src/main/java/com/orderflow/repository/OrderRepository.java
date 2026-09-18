package com.orderflow.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderflow.domain.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
	List<Order> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
	java.util.Optional<Order> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
