package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.JournalLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalLineRepository extends JpaRepository<JournalLineEntity, UUID> {
}
