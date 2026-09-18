package com.orderflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderflow.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByOrganizationId(UUID organizationId);
    Optional<Product> findByIdAndOrganizationId(UUID id, UUID organizationId);
    boolean existsByIdAndOrganizationId(UUID id, UUID organizationId);
}
