CREATE TABLE IF NOT EXISTS payment_intents (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(30) NOT NULL,
    client_secret VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    payment_method_token VARCHAR(255),
    authorization_code VARCHAR(255),
    transaction_id VARCHAR(255),
    failure_reason VARCHAR(255),
    is_test BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
