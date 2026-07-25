CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id UUID PRIMARY KEY,
    entry_id VARCHAR(255) NOT NULL UNIQUE,
    merchant_id VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'EUR',
    is_test BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ledger_merchant_account ON ledger_entries (merchant_id, account_type, is_test);
CREATE INDEX IF NOT EXISTS idx_ledger_reference ON ledger_entries (reference_id);
