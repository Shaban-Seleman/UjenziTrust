package com.uzenjitrust.ops.repo;

import com.uzenjitrust.ops.domain.EscrowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EscrowRepository extends JpaRepository<EscrowEntity, UUID> {

    Page<EscrowEntity> findByPayerUserIdOrBeneficiaryUserId(UUID payerUserId, UUID beneficiaryUserId, Pageable pageable);

    Optional<EscrowEntity> findByBusinessKey(String businessKey);
}
