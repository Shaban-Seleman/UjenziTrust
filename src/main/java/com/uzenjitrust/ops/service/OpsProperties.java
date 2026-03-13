package com.uzenjitrust.ops.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ops")
public class OpsProperties {

    private int outboxBatchSize = 20;
    private long outboxDispatchDelayMs = 5000;
    private int outboxMaxAutoRetryAttempts = 5;
    private int retentionDays = 14;
    private int webhookReplayWindowSeconds = 300;
    private String webhookSecret = "change-me-webhook-secret";

    public int getOutboxBatchSize() {
        return outboxBatchSize;
    }

    public void setOutboxBatchSize(int outboxBatchSize) {
        this.outboxBatchSize = outboxBatchSize;
    }

    public long getOutboxDispatchDelayMs() {
        return outboxDispatchDelayMs;
    }

    public void setOutboxDispatchDelayMs(long outboxDispatchDelayMs) {
        this.outboxDispatchDelayMs = outboxDispatchDelayMs;
    }

    public int getOutboxMaxAutoRetryAttempts() {
        return outboxMaxAutoRetryAttempts;
    }

    public void setOutboxMaxAutoRetryAttempts(int outboxMaxAutoRetryAttempts) {
        this.outboxMaxAutoRetryAttempts = outboxMaxAutoRetryAttempts;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public int getWebhookReplayWindowSeconds() {
        return webhookReplayWindowSeconds;
    }

    public void setWebhookReplayWindowSeconds(int webhookReplayWindowSeconds) {
        this.webhookReplayWindowSeconds = webhookReplayWindowSeconds;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
