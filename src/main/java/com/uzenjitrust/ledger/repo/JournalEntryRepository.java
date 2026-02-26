package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, UUID> {

    Optional<JournalEntryEntity> findByEntryTypeAndReferenceIdAndIdempotencyKey(String entryType, String referenceId, String idempotencyKey);
}
