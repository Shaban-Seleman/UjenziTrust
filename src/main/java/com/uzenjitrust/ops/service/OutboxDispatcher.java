package com.uzenjitrust.ops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.ops.bank.BankAdapter;
import com.uzenjitrust.ops.bank.BankPayoutRequest;
import com.uzenjitrust.ops.domain.OutboxEventEntity;
import com.uzenjitrust.ops.domain.OutboxStatus;
import com.uzenjitrust.ops.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@Profile("!test")
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository outboxRepository;
    private final DisbursementService disbursementService;
    private final BankAdapter bankAdapter;
    private final OpsProperties opsProperties;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(OutboxEventRepository outboxRepository,
                            DisbursementService disbursementService,
                            BankAdapter bankAdapter,
                            OpsProperties opsProperties,
                            ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.disbursementService = disbursementService;
        this.bankAdapter = bankAdapter;
        this.opsProperties = opsProperties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.ops.outbox-dispatch-delay-ms:5000}")
    @Transactional
    public void dispatch() {
        List<OutboxEventEntity> batch = outboxRepository.lockPendingBatch(opsProperties.getOutboxBatchSize());
        for (OutboxEventEntity event : batch) {
            try {
                PayoutOutboxPayload payload = objectMapper.treeToValue(event.getPayload(), PayoutOutboxPayload.class);
                var response = bankAdapter.initiatePayout(new BankPayoutRequest(
                        payload.disbursementId(),
                        payload.payeeId(),
                        new BigDecimal(payload.amount()),
                        payload.currency(),
                        payload.idempotencyKey()
                ));

                disbursementService.markSubmitted(payload.disbursementId(), response.bankReference());
                event.setStatus(OutboxStatus.SENT);
                event.setLastError(null);
            } catch (Exception ex) {
                int retry = event.getRetryCount() + 1;
                event.setRetryCount(retry);
                event.setStatus(OutboxStatus.PENDING);
                event.setLastError(ex.getMessage());
                long delaySeconds = Math.min(300, (long) Math.pow(2, Math.min(retry, 8)));
                event.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
                log.warn("Outbox dispatch failed eventId={} retry={} error={}", event.getId(), retry, ex.getMessage());
            }
        }
    }
}
