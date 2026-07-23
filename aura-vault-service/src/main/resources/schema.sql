CREATE TABLE IF NOT EXISTS card_tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    encrypted_pan VARCHAR(512) NOT NULL,
    encrypted_cvv VARCHAR(256) NOT NULL,
    cardholder_name VARCHAR(255) NOT NULL,
    expiration_month INT NOT NULL,
    expiration_year INT NOT NULL,
    card_brand VARCHAR(50) NOT NULL,
    masked_pan VARCHAR(20) NOT NULL,
    is_test BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_card_tokens_token ON card_tokens(token);
CREATE INDEX IF NOT EXISTS idx_card_tokens_expires_at ON card_tokens(expires_at);
