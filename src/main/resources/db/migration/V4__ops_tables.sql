CREATE TABLE ops.escrows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_key VARCHAR(128) NOT NULL UNIQUE,
    escrow_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    total_amount NUMERIC(18,2) NOT NULL,
    payer_user_id UUID,
    beneficiary_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_escrows_status ON ops.escrows (status);

CREATE TABLE ops.disbursement_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_key VARCHAR(128) NOT NULL UNIQUE,
    escrow_id UUID REFERENCES ops.escrows(id),
    milestone_id UUID,
    payee_type VARCHAR(64) NOT NULL,
    payee_id UUID NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    status VARCHAR(32) NOT NULL,
    settlement_ref VARCHAR(128),
    bank_reference VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_disbursement_amount_positive CHECK (amount > 0)
);

CREATE UNIQUE INDEX uq_disbursement_settlement_ref
    ON ops.disbursement_orders (settlement_ref)
    WHERE settlement_ref IS NOT NULL;
CREATE INDEX idx_disbursement_milestone_id ON ops.disbursement_orders (milestone_id);
CREATE INDEX idx_disbursement_status ON ops.disbursement_orders (status);
CREATE INDEX idx_disbursement_milestone_status ON ops.disbursement_orders (milestone_id, status);

CREATE TABLE ops.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    retry_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_dispatch ON ops.outbox_events (status, next_attempt_at);

CREATE TABLE ops.webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(64) NOT NULL,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    signature VARCHAR(256) NOT NULL,
    event_ts TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_source_status ON ops.webhook_events (source, status);
