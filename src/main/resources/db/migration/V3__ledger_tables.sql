CREATE TABLE ledger.accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ledger.journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_type VARCHAR(64) NOT NULL,
    reference_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    description TEXT,
    actor_user_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ledger_entry_idempotency UNIQUE (entry_type, reference_id, idempotency_key)
);

CREATE INDEX idx_journal_entries_created_at ON ledger.journal_entries (created_at);
CREATE INDEX idx_journal_entries_reference ON ledger.journal_entries (reference_id);

CREATE TABLE ledger.journal_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID NOT NULL REFERENCES ledger.journal_entries(id),
    account_id UUID NOT NULL REFERENCES ledger.accounts(id),
    line_type VARCHAR(8) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TZS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_journal_line_type CHECK (line_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_journal_line_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_journal_lines_entry_id ON ledger.journal_lines (journal_entry_id);
CREATE INDEX idx_journal_lines_account_id ON ledger.journal_lines (account_id);

CREATE TABLE ledger.hash_chain (
    id BIGSERIAL PRIMARY KEY,
    journal_entry_id UUID NOT NULL UNIQUE REFERENCES ledger.journal_entries(id),
    chain_index BIGINT NOT NULL UNIQUE,
    prev_hash VARCHAR(64),
    hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ledger.idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key_scope VARCHAR(64) NOT NULL,
    key_value VARCHAR(128) NOT NULL,
    request_fingerprint VARCHAR(128) NOT NULL,
    response_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ledger_idem UNIQUE (key_scope, key_value)
);

CREATE OR REPLACE FUNCTION ledger.prevent_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Ledger tables are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_update_journal_entries
BEFORE UPDATE OR DELETE ON ledger.journal_entries
FOR EACH ROW EXECUTE FUNCTION ledger.prevent_mutation();

CREATE TRIGGER trg_prevent_update_journal_lines
BEFORE UPDATE OR DELETE ON ledger.journal_lines
FOR EACH ROW EXECUTE FUNCTION ledger.prevent_mutation();

CREATE TRIGGER trg_prevent_update_hash_chain
BEFORE UPDATE OR DELETE ON ledger.hash_chain
FOR EACH ROW EXECUTE FUNCTION ledger.prevent_mutation();
