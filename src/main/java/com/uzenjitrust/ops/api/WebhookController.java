package com.uzenjitrust.ops.api;

import com.uzenjitrust.ops.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ops/webhooks")
@Tag(name = "Ops Webhooks")
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/settlement")
    @Operation(summary = "Process bank settlement webhook")
    public ResponseEntity<WebhookAckResponse> settlementWebhook(@RequestBody SettlementWebhookRequest request,
                                                                @RequestHeader("X-Signature") String signature) {
        String rawBody = toRawJson(request);
        String status = webhookService.processSettlementEvent(request, signature, rawBody);
        return ResponseEntity.ok(new WebhookAckResponse(status, request.eventId()));
    }

    private String toRawJson(SettlementWebhookRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid webhook payload", ex);
        }
    }
}
