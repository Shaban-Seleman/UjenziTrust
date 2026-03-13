package com.uzenjitrust.ops.repo;

import com.uzenjitrust.ops.domain.WebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, UUID>, JpaSpecificationExecutor<WebhookEventEntity> {

    Optional<WebhookEventEntity> findByEventId(String eventId);
}
