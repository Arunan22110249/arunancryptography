package com.orderflow.repository;

import com.orderflow.domain.IdempotencyKeyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKeyRecord, UUID> {
    Optional<IdempotencyKeyRecord> findByKeyValue(String keyValue);
}
