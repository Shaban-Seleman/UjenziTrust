package com.uzenjitrust.build.repo;

import com.uzenjitrust.build.domain.ProjectEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    Page<ProjectEntity> findByOwnerUserIdOrContractorUserIdOrInspectorUserId(UUID ownerUserId,
                                                                              UUID contractorUserId,
                                                                              UUID inspectorUserId,
                                                                              Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from ProjectEntity p where p.id = :id")
    Optional<ProjectEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
