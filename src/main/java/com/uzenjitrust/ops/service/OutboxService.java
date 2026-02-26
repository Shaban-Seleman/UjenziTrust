package com.uzenjitrust.ops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.OutboxStatus;
import com.uzenjitrust.ops.repo.OutboxEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEventEntity createEventIdempotent(String aggregateType,
                                                   String aggregateId,
                                                   String eventType,
                                                   Object payload,
                                                   String idempotencyKey) {
        return outboxRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() -> {
            OutboxEventEntity event = new OutboxEventEntity();
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setPayload(objectMapper.valueToTree(payload));
            event.setStatus(OutboxStatus.PENDING);
            event.setIdempotencyKey(idempotencyKey);
            event.setRetryCount(0);
            event.setNextAttemptAt(Instant.now());
            try {
                return outboxRepository.save(event);
            } catch (DataIntegrityViolationException ex) {
                return outboxRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> ex);
            }
        });
    }
}
