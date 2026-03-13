package com.uzenjitrust.ops.repo;

import com.uzenjitrust.ops.domain.OperatorAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface OperatorAuditEventRepository extends JpaRepository<OperatorAuditEventEntity, UUID>, JpaSpecificationExecutor<OperatorAuditEventEntity> {
}
