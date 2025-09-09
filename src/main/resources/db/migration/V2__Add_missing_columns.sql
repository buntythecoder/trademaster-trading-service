-- V2__Add_missing_columns.sql
-- Add missing columns for risk_limits and orders tables

-- Add missing columns to risk_limits table
ALTER TABLE risk_limits 
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true,
ADD COLUMN IF NOT EXISTS allow_margin_trading BOOLEAN NOT NULL DEFAULT false;

-- Add missing columns to orders table  
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS total_filled_value NUMERIC(18,4),
ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);

-- Create index on active column for better performance
CREATE INDEX IF NOT EXISTS idx_risk_limits_active ON risk_limits(active);

-- Add comments for new columns
COMMENT ON COLUMN risk_limits.active IS 'Whether the risk limit is active/enabled';
COMMENT ON COLUMN risk_limits.allow_margin_trading IS 'Whether margin trading is allowed for this user';
COMMENT ON COLUMN orders.total_filled_value IS 'Total monetary value of filled quantity';
COMMENT ON COLUMN orders.rejection_reason IS 'Reason why order was rejected (if applicable)';

-- Update existing data to ensure consistency
UPDATE risk_limits SET active = true WHERE active IS NULL;
UPDATE risk_limits SET allow_margin_trading = false WHERE allow_margin_trading IS NULL;