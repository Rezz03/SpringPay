-- V1__initial_schema.sql
-- Description: Create initial database schema for SpringPay Payment Gateway
-- Author: Perez
-- Date: 2025-11-18

-- Create merchants table
CREATE TABLE merchants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    api_key_hash VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_merchant_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'))
);

-- Create email_verification_tokens table
CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
);

-- Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
);

-- Create api_keys table (for multiple keys per merchant)
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    label VARCHAR(100),
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_apikey_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
);

-- Create payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    refund_reason VARCHAR(500),
    refunded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT fk_payment_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id)
);

-- Create transactions table (audit trail)
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transaction_action CHECK (action IN ('CREATE', 'STATUS_UPDATE', 'REFUND')),
    CONSTRAINT fk_transaction_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- Create webhook_logs table
CREATE TABLE webhook_logs (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    webhook_url VARCHAR(500) NOT NULL,
    payload TEXT NOT NULL,
    response_status INT,
    response_body TEXT,
    attempt_number INT NOT NULL DEFAULT 1,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    error_message VARCHAR(1000),
    delivered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- Create indexes for performance
CREATE INDEX idx_merchants_email ON merchants(email);
CREATE INDEX idx_merchants_status ON merchants(status);
CREATE INDEX idx_merchants_created_at ON merchants(created_at);

CREATE INDEX idx_verification_token ON email_verification_tokens(token);
CREATE INDEX idx_verification_merchant ON email_verification_tokens(merchant_id);
CREATE INDEX idx_verification_expires ON email_verification_tokens(expires_at);

CREATE INDEX idx_reset_token ON password_reset_tokens(token);
CREATE INDEX idx_reset_merchant ON password_reset_tokens(merchant_id);
CREATE INDEX idx_reset_expires ON password_reset_tokens(expires_at);

CREATE INDEX idx_apikeys_merchant ON api_keys(merchant_id);
CREATE INDEX idx_apikeys_revoked ON api_keys(revoked);

CREATE INDEX idx_payments_merchant ON payments(merchant_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);
CREATE INDEX idx_payments_merchant_status ON payments(merchant_id, status);

CREATE INDEX idx_transactions_payment ON transactions(payment_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

CREATE INDEX idx_webhooks_payment ON webhook_logs(payment_id);
CREATE INDEX idx_webhooks_success ON webhook_logs(success);
CREATE INDEX idx_webhooks_delivered_at ON webhook_logs(delivered_at);

-- Create function to update updated_at timestamp automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic updated_at
CREATE TRIGGER update_merchants_updated_at BEFORE UPDATE ON merchants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert comments for documentation
COMMENT ON TABLE merchants IS 'Stores merchant registration and authentication information';
COMMENT ON TABLE payments IS 'Stores payment transaction records with lifecycle status';
COMMENT ON TABLE transactions IS 'Audit trail of all payment lifecycle changes';
COMMENT ON TABLE webhook_logs IS 'Logs of webhook notification delivery attempts';
COMMENT ON TABLE email_verification_tokens IS 'Tokens for email address verification';
COMMENT ON TABLE password_reset_tokens IS 'Tokens for password reset flow';
COMMENT ON TABLE api_keys IS 'API keys for merchant authentication';