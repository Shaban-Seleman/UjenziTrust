package com.uzenjitrust.build.repo;

import com.uzenjitrust.build.domain.MilestoneSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MilestoneSubmissionRepository extends JpaRepository<MilestoneSubmissionEntity, UUID> {
}
