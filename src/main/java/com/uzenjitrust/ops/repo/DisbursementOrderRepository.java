package com.uzenjitrust.ops.repo;

import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.DisbursementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisbursementOrderRepository extends JpaRepository<DisbursementOrderEntity, UUID>, JpaSpecificationExecutor<DisbursementOrderEntity> {

    Optional<DisbursementOrderEntity> findByBusinessKey(String businessKey);

    Optional<DisbursementOrderEntity> findBySettlementRef(String settlementRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from DisbursementOrderEntity d where d.id = :id")
    Optional<DisbursementOrderEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    long countByMilestoneIdAndStatusNot(UUID milestoneId, DisbursementStatus status);

    List<DisbursementOrderEntity> findByMilestoneId(UUID milestoneId);

    Page<DisbursementOrderEntity> findByEscrow_Id(UUID escrowId, Pageable pageable);
}
