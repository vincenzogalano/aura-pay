CREATE TABLE IF NOT EXISTS payment_intents (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(30) NOT NULL,
    client_secret VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    customer_email VARCHAR(255),
    refunded_amount_cents BIGINT NOT NULL DEFAULT 0,
    payment_method_token VARCHAR(255),
    authorization_code VARCHAR(255),
    transaction_id VARCHAR(255),
    failure_reason VARCHAR(255),
    is_test BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS external_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
