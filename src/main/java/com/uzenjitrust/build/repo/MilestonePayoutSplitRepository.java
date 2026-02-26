package com.uzenjitrust.build.repo;

import com.uzenjitrust.build.domain.MilestonePayoutSplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MilestonePayoutSplitRepository extends JpaRepository<MilestonePayoutSplitEntity, UUID> {

    Optional<MilestonePayoutSplitEntity> findByBusinessKey(String businessKey);

    List<MilestonePayoutSplitEntity> findByMilestone_Id(UUID milestoneId);
}
