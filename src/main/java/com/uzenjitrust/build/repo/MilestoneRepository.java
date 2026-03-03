package com.uzenjitrust.build.repo;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MilestoneRepository extends JpaRepository<MilestoneEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MilestoneEntity m where m.id = :id")
    Optional<MilestoneEntity> findByIdForUpdate(@Param("id") UUID id);

    List<MilestoneEntity> findByProject_IdOrderBySequenceNoAsc(UUID projectId);

    @Query("""
            select m.id
            from MilestoneEntity m
            where m.status = :status
              and m.retentionReleaseAt is not null
              and m.retentionReleaseAt <= :retentionReleaseAt
              and m.retentionAmount > 0
              and m.retentionReleasedAt is null
            """)
    List<UUID> findIdsEligibleForRetentionRelease(@Param("status") MilestoneStatus status,
                                                  @Param("retentionReleaseAt") Instant retentionReleaseAt);
}
