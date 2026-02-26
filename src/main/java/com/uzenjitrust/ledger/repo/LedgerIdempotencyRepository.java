package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.LedgerIdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerIdempotencyRepository extends JpaRepository<LedgerIdempotencyKeyEntity, UUID> {

    Optional<LedgerIdempotencyKeyEntity> findByKeyScopeAndKeyValue(String keyScope, String keyValue);
}
