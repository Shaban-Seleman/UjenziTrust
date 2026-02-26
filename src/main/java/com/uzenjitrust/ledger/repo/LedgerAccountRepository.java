package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.LedgerAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, UUID> {

    Optional<LedgerAccountEntity> findByAccountCodeAndActiveTrue(String accountCode);
}
