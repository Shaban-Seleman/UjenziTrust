package com.uzenjitrust.ledger.repo;

import com.uzenjitrust.ledger.domain.HashChainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface HashChainRepository extends JpaRepository<HashChainEntity, Long> {

    @Modifying(flushAutomatically = true)
    @Query(value = "lock table ledger.hash_chain in exclusive mode", nativeQuery = true)
    void lockChainTable();

    @Query(value = "select * from ledger.hash_chain order by chain_index desc limit 1", nativeQuery = true)
    Optional<HashChainEntity> findTail();

    Optional<HashChainEntity> findByJournalEntry_Id(UUID journalEntryId);
}
