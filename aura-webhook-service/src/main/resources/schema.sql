CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL UNIQUE,
    target_url VARCHAR(512) NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_events VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id UUID PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    merchant_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    target_url VARCHAR(512) NOT NULL,
    payload TEXT NOT NULL,
    http_status INTEGER,
    response_body TEXT,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    status VARCHAR(30) NOT NULL,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_test BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_merchant ON webhook_deliveries(merchant_id);
CREATE INDEX IF NOT EXISTS idx_webhook_deliveries_retry ON webhook_deliveries(status, next_retry_at);
