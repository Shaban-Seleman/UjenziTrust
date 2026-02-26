package com.uzenjitrust.build.repo;

import com.uzenjitrust.build.domain.InspectionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface InspectionRepository extends JpaRepository<InspectionEntity, UUID> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InspectionEntity> findById(UUID id);
}
