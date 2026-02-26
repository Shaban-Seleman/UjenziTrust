package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.HashChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface HashChainRepository extends JpaRepository<HashChainEntity, Long> {

    @Query(value = "select * from ledger.hash_chain order by chain_index desc limit 1 for update", nativeQuery = true)
    Optional<HashChainEntity> findTailForUpdate();

    Optional<HashChainEntity> findByJournalEntry_Id(UUID journalEntryId);
}
