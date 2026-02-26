package com.uzenjitrust.ops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.ledger.service.LedgerAccountCodes;
import com.uzenjitrust.ops.api.SettlementWebhookRequest;
import com.uzenjitrust.ops.domain.WebhookEventEntity;
import com.uzenjitrust.ops.domain.WebhookStatus;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.repo.WebhookEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WebhookService {

    private final WebhookEventRepository webhookRepository;
    private final DisbursementOrderRepository disbursementRepository;
    private final DisbursementService disbursementService;
    private final WebhookVerificationService verificationService;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookEventRepository webhookRepository,
                          DisbursementOrderRepository disbursementRepository,
                          DisbursementService disbursementService,
                          WebhookVerificationService verificationService,
                          ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.disbursementRepository = disbursementRepository;
        this.disbursementService = disbursementService;
        this.verificationService = verificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String processSettlementEvent(SettlementWebhookRequest request,
                                         String signature,
                                         String rawBody) {
        if (request == null || request.eventId() == null || request.eventType() == null || request.payload() == null) {
            throw new BadRequestException("Invalid webhook payload");
        }

        verificationService.verify(signature, request.eventTs(), rawBody);

        if (webhookRepository.findByEventId(request.eventId()).isPresent()) {
            return "DUPLICATE";
        }

        WebhookEventEntity event = new WebhookEventEntity();
        event.setSource("MOCK_BANK");
        event.setEventId(request.eventId());
        event.setEventType(request.eventType());
        event.setPayload(objectMapper.valueToTree(request));
        event.setSignature(signature);
        event.setEventTs(request.eventTs());
        event.setStatus(WebhookStatus.RECEIVED);

        try {
            webhookRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            return "DUPLICATE";
        }

        if (!"DISBURSEMENT_SETTLED".equals(request.eventType())) {
            event.setStatus(WebhookStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            return "IGNORED";
        }

        var disbursement = disbursementRepository.findById(request.payload().disbursementId())
                .orElseThrow(() -> new BadRequestException("Disbursement not found for settlement event"));

        disbursementService.settleDisbursement(
                disbursement.getId(),
                request.payload().settlementRef(),
                null,
                payableAccount(disbursement.getPayeeType()),
                request.eventId()
        );

        event.setStatus(WebhookStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        return "PROCESSED";
    }

    private String payableAccount(String payeeType) {
        return switch (payeeType) {
            case "CONTRACTOR" -> LedgerAccountCodes.PAYABLE_CONTRACTOR;
            case "INSPECTOR" -> LedgerAccountCodes.PAYABLE_INSPECTOR;
            case "SELLER" -> LedgerAccountCodes.PAYABLE_SELLER;
            case "RETENTION" -> LedgerAccountCodes.PAYABLE_RETENTION;
            default -> throw new BadRequestException("Unknown payeeType: " + payeeType);
        };
    }

}
