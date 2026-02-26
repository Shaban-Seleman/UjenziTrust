package com.uzenjitrust.ops.service;

import com.uzenjitrust.common.error.ForbiddenException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class WebhookVerificationService {

    private final OpsProperties opsProperties;

    public WebhookVerificationService(OpsProperties opsProperties) {
        this.opsProperties = opsProperties;
    }

    public void verify(String signatureHeader, Instant eventTs, String rawBody) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ForbiddenException("Missing webhook signature");
        }
        if (eventTs == null) {
            throw new ForbiddenException("Missing event timestamp");
        }

        long drift = Math.abs(Duration.between(eventTs, Instant.now()).getSeconds());
        if (drift > opsProperties.getWebhookReplayWindowSeconds()) {
            throw new ForbiddenException("Webhook timestamp outside replay window");
        }

        String expected = computeHmac(eventTs.toString() + "." + rawBody, opsProperties.getWebhookSecret());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Invalid webhook signature");
        }
    }

    private String computeHmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify webhook signature", ex);
        }
    }
}
