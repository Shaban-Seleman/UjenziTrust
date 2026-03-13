package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, UUID>, JpaSpecificationExecutor<JournalEntryEntity> {

    Optional<JournalEntryEntity> findByEntryTypeAndReferenceIdAndIdempotencyKey(String entryType, String referenceId, String idempotencyKey);
}
