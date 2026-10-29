-- V2__income_expense_and_financial_transactions.sql
-- Migration for Cambodian Farmer Income & Expense Management with Categorized Transactions

CREATE TABLE IF NOT EXISTS farm_transactions (
    id BIGSERIAL PRIMARY KEY,
    farmer_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    category VARCHAR(40) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD' NOT NULL,
    exchange_rate_to_usd NUMERIC(10, 4) DEFAULT 1.0000 NOT NULL,
    amount_in_usd NUMERIC(14, 2) NOT NULL,
    transaction_date DATE NOT NULL,
    payment_method VARCHAR(30),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    description TEXT,
    description_kh TEXT,
    receipt_image TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_tx_farmer FOREIGN KEY (farmer_id) REFERENCES farmers(id) ON DELETE CASCADE
);

-- Indexes for performance and reporting
CREATE INDEX IF NOT EXISTS idx_tx_farmer_date ON farm_transactions(farmer_id, transaction_date DESC);
CREATE INDEX IF NOT EXISTS idx_tx_farmer_type ON farm_transactions(farmer_id, transaction_type);
CREATE INDEX IF NOT EXISTS idx_tx_category ON farm_transactions(farmer_id, category);
