package com.uzenjitrust.build.repo;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MilestoneRepository extends JpaRepository<MilestoneEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from MilestoneEntity m where m.id = :id")
    Optional<MilestoneEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    List<MilestoneEntity> findByProject_IdOrderBySequenceNoAsc(UUID projectId);

    List<MilestoneEntity> findByStatusAndRetentionReleaseAtLessThanEqual(MilestoneStatus status, Instant retentionReleaseAt);
}
