ALTER TABLE build.milestones
    ADD COLUMN IF NOT EXISTS retention_released_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_milestones_retention_released_at
    ON build.milestones (retention_released_at);

UPDATE ledger.accounts
SET account_code = '2080',
    name = 'Retention Payable'
WHERE account_code = '2040';

UPDATE ledger.accounts
SET account_code = '2040',
    name = 'Inspector Payable'
WHERE account_code = '2030';

UPDATE ledger.accounts
SET account_code = '2030',
    name = 'Contractor Payable'
WHERE account_code = '2020';

INSERT INTO ledger.accounts (id, account_code, name, account_type, currency, active)
VALUES (gen_random_uuid(), '2090', 'Supplier Payable', 'LIABILITY', 'TZS', TRUE)
ON CONFLICT (account_code) DO NOTHING;
