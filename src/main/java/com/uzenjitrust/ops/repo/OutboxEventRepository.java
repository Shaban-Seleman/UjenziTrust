package com.uzenjitrust.ops.repo;

import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    Optional<OutboxEventEntity> findByIdempotencyKey(String idempotencyKey);

    @Query(value = """
        select * from ops.outbox_events
        where status = 'PENDING' and next_attempt_at <= now()
        order by created_at asc
        limit :batchSize
        for update skip locked
        """, nativeQuery = true)
    List<OutboxEventEntity> lockPendingBatch(@Param("batchSize") int batchSize);

    long countByStatus(OutboxStatus status);
}
