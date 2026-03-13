package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.JournalLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JournalLineRepository extends JpaRepository<JournalLineEntity, UUID> {

    List<JournalLineEntity> findByJournalEntry_IdOrderByCreatedAtAsc(UUID journalEntryId);
}
