-- V1__inventory_and_product_enhancements.sql
-- Migration for Cambodian Agricultural Marketplace Product enhancements & Inventory Movements

-- 1. Enhance products table with unit, localization, status, threshold, and optimistic locking version
ALTER TABLE products ADD COLUMN IF NOT EXISTS name_kh VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS description_kh TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS unit VARCHAR(50) DEFAULT 'KG' NOT NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS low_stock_threshold INT DEFAULT 10 NOT NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. Inventory movements table
CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    previous_stock INT NOT NULL,
    new_stock INT NOT NULL,
    cost_per_unit NUMERIC(12, 2),
    reference_type VARCHAR(50),
    reference_id BIGINT,
    note TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_farmer FOREIGN KEY (farmer_id) REFERENCES farmers(id) ON DELETE CASCADE
);

-- 3. Indexes for fast tenant-scoped queries, audits, and low inventory checks
CREATE INDEX IF NOT EXISTS idx_inv_farmer_created ON inventory_movements(farmer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inv_product_created ON inventory_movements(product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_farmer_status ON products(farmer_id, status);
CREATE INDEX IF NOT EXISTS idx_products_low_stock ON products(farmer_id, quantity, low_stock_threshold);
