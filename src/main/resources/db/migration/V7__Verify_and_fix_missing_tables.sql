-- V7__Verify_and_fix_missing_tables.sql
-- Verification script to ensure all required tables exist for trading-service entities

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Ensure order_fills table exists (this should be from V1 but let's make sure)
CREATE TABLE IF NOT EXISTS order_fills (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    fill_quantity INTEGER NOT NULL CHECK (fill_quantity > 0),
    fill_price DECIMAL(15,4) NOT NULL CHECK (fill_price > 0),
    fill_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    broker_fill_id VARCHAR(100),
    commission DECIMAL(10,4) DEFAULT 0 CHECK (commission >= 0),
    taxes DECIMAL(10,4) DEFAULT 0 CHECK (taxes >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create foreign key constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'order_fills_order_id_fkey'
        AND table_name = 'order_fills'
    ) THEN
        -- First check if orders table exists
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN
            ALTER TABLE order_fills
            ADD CONSTRAINT order_fills_order_id_fkey
            FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

-- Ensure indexes exist for order_fills
CREATE INDEX IF NOT EXISTS idx_order_fills_order_id ON order_fills(order_id);
CREATE INDEX IF NOT EXISTS idx_order_fills_fill_time ON order_fills(fill_time);

-- Verify all required trading service tables exist
DO $$
BEGIN
    -- Check for order_fills table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_fills') THEN
        RAISE EXCEPTION 'CRITICAL: order_fills table is missing';
    ELSE
        RAISE NOTICE 'SUCCESS: order_fills table exists';
    END IF;

    -- Check for orders table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN
        RAISE NOTICE 'WARNING: orders table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: orders table exists';
    END IF;

    -- Check for simple_orders table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'simple_orders') THEN
        RAISE NOTICE 'WARNING: simple_orders table is missing (expected from V6)';
    ELSE
        RAISE NOTICE 'SUCCESS: simple_orders table exists';
    END IF;

    -- Check for trades table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trades') THEN
        RAISE NOTICE 'WARNING: trades table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: trades table exists';
    END IF;

    -- Check for portfolios table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'portfolios') THEN
        RAISE NOTICE 'WARNING: portfolios table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: portfolios table exists';
    END IF;

    -- Check for positions table (from V3)
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'positions') THEN
        RAISE NOTICE 'WARNING: positions table is missing (expected from V3)';
    ELSE
        RAISE NOTICE 'SUCCESS: positions table exists';
    END IF;

    -- Check for risk_limits table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'risk_limits') THEN
        RAISE NOTICE 'WARNING: risk_limits table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: risk_limits table exists';
    END IF;

    -- Check for trading_audit_log table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'trading_audit_log') THEN
        RAISE NOTICE 'WARNING: trading_audit_log table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: trading_audit_log table exists';
    END IF;

    -- Check for portfolio_history table
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'portfolio_history') THEN
        RAISE NOTICE 'WARNING: portfolio_history table is missing (expected from V1)';
    ELSE
        RAISE NOTICE 'SUCCESS: portfolio_history table exists';
    END IF;

    RAISE NOTICE 'Table verification completed for trading-service';
END $$;

-- Update column comments for order_fills to match entity
COMMENT ON TABLE order_fills IS 'Order fill tracking for partial executions and transaction details';
COMMENT ON COLUMN order_fills.id IS 'Primary key for order fill record';
COMMENT ON COLUMN order_fills.order_id IS 'Foreign key reference to orders.id';
COMMENT ON COLUMN order_fills.fill_quantity IS 'Quantity filled in this execution (must be positive)';
COMMENT ON COLUMN order_fills.fill_price IS 'Execution price for this fill (must be positive)';
COMMENT ON COLUMN order_fills.fill_time IS 'Timestamp when fill was executed';
COMMENT ON COLUMN order_fills.broker_fill_id IS 'Broker internal fill identifier';
COMMENT ON COLUMN order_fills.commission IS 'Commission charged for this fill';
COMMENT ON COLUMN order_fills.taxes IS 'Taxes charged for this fill';
COMMENT ON COLUMN order_fills.created_at IS 'Record creation timestamp';