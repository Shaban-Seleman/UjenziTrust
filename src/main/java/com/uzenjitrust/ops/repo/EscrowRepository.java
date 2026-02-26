package com.uzenjitrust.ops.repo;

import com.uzenjitrust.ops.domain.EscrowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EscrowRepository extends JpaRepository<EscrowEntity, UUID> {

    Optional<EscrowEntity> findByBusinessKey(String businessKey);
}
