INSERT INTO ledger.accounts (id, account_code, name, account_type, currency, active)
VALUES
    (gen_random_uuid(), '1010', 'Bank Cash', 'ASSET', 'TZS', TRUE),
    (gen_random_uuid(), '2010', 'Escrow Liability', 'LIABILITY', 'TZS', TRUE),
    (gen_random_uuid(), '2020', 'Contractor Payable', 'LIABILITY', 'TZS', TRUE),
    (gen_random_uuid(), '2030', 'Inspector Payable', 'LIABILITY', 'TZS', TRUE),
    (gen_random_uuid(), '2040', 'Retention Payable', 'LIABILITY', 'TZS', TRUE),
    (gen_random_uuid(), '2050', 'Seller Payable', 'LIABILITY', 'TZS', TRUE)
ON CONFLICT (account_code) DO NOTHING;
