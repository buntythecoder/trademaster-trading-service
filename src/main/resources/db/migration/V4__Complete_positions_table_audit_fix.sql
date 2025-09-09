-- V4__Complete_positions_table_audit_fix.sql
-- Comprehensive audit and fix for positions table to match Position entity exactly
-- This migration ensures all columns match the entity definition with proper constraints

-- First, let's ensure all columns exist with correct data types and constraints

-- Fix nullable constraints for required fields
ALTER TABLE positions ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE positions ALTER COLUMN symbol SET NOT NULL;
ALTER TABLE positions ALTER COLUMN exchange SET NOT NULL;
ALTER TABLE positions ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE positions ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE positions ALTER COLUMN updated_at SET NOT NULL;

-- Fix column lengths to match entity specifications
ALTER TABLE positions ALTER COLUMN symbol TYPE VARCHAR(20);
ALTER TABLE positions ALTER COLUMN exchange TYPE VARCHAR(10);
ALTER TABLE positions ALTER COLUMN side TYPE VARCHAR(10);
ALTER TABLE positions ALTER COLUMN sector TYPE VARCHAR(50);
ALTER TABLE positions ALTER COLUMN industry TYPE VARCHAR(50);
ALTER TABLE positions ALTER COLUMN asset_class TYPE VARCHAR(20);
ALTER TABLE positions ALTER COLUMN tags TYPE VARCHAR(200);

-- Fix numeric precision and scale to match entity specifications
ALTER TABLE positions ALTER COLUMN average_cost TYPE NUMERIC(15,4);
ALTER TABLE positions ALTER COLUMN cost_basis TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN current_price TYPE NUMERIC(15,4);
ALTER TABLE positions ALTER COLUMN market_value TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN unrealized_pnl TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN unrealized_pnl_percent TYPE NUMERIC(8,4);
ALTER TABLE positions ALTER COLUMN realized_pnl TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN total_pnl TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN intraday_pnl TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN previous_close_value TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN day_change TYPE NUMERIC(20,4);
ALTER TABLE positions ALTER COLUMN day_change_percent TYPE NUMERIC(8,4);
ALTER TABLE positions ALTER COLUMN margin_requirement TYPE NUMERIC(15,4);
ALTER TABLE positions ALTER COLUMN margin_utilization TYPE NUMERIC(5,2);
ALTER TABLE positions ALTER COLUMN borrowing_cost TYPE NUMERIC(15,4);
ALTER TABLE positions ALTER COLUMN risk_score TYPE NUMERIC(5,4);
ALTER TABLE positions ALTER COLUMN portfolio_weight TYPE NUMERIC(5,2);
ALTER TABLE positions ALTER COLUMN beta TYPE NUMERIC(8,4);

-- Add missing constraints from entity
ALTER TABLE positions ADD CONSTRAINT positions_quantity_default CHECK (quantity IS NOT NULL);
ALTER TABLE positions ADD CONSTRAINT positions_pending_quantity_default CHECK (pending_quantity IS NOT NULL);
ALTER TABLE positions ADD CONSTRAINT positions_realized_pnl_default CHECK (realized_pnl IS NOT NULL);
ALTER TABLE positions ADD CONSTRAINT positions_trade_count_default CHECK (trade_count IS NOT NULL);
ALTER TABLE positions ADD CONSTRAINT positions_borrowing_cost_default CHECK (borrowing_cost IS NOT NULL);

-- Set default values for columns that should have defaults
ALTER TABLE positions ALTER COLUMN quantity SET DEFAULT 0;
ALTER TABLE positions ALTER COLUMN pending_quantity SET DEFAULT 0;
ALTER TABLE positions ALTER COLUMN realized_pnl SET DEFAULT 0;
ALTER TABLE positions ALTER COLUMN trade_count SET DEFAULT 0;
ALTER TABLE positions ALTER COLUMN borrowing_cost SET DEFAULT 0;

-- Update existing NULL values to match defaults
UPDATE positions SET quantity = 0 WHERE quantity IS NULL;
UPDATE positions SET pending_quantity = 0 WHERE pending_quantity IS NULL;
UPDATE positions SET realized_pnl = 0 WHERE realized_pnl IS NULL;
UPDATE positions SET trade_count = 0 WHERE trade_count IS NULL;
UPDATE positions SET borrowing_cost = 0 WHERE borrowing_cost IS NULL;

-- Add business logic constraints
ALTER TABLE positions ADD CONSTRAINT positions_margin_requirement_positive 
    CHECK (margin_requirement IS NULL OR margin_requirement >= 0);
    
ALTER TABLE positions ADD CONSTRAINT positions_margin_utilization_valid 
    CHECK (margin_utilization IS NULL OR (margin_utilization >= 0 AND margin_utilization <= 100));
    
ALTER TABLE positions ADD CONSTRAINT positions_risk_score_valid 
    CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 1));
    
ALTER TABLE positions ADD CONSTRAINT positions_portfolio_weight_valid 
    CHECK (portfolio_weight IS NULL OR (portfolio_weight >= 0 AND portfolio_weight <= 100));
    
ALTER TABLE positions ADD CONSTRAINT positions_trade_count_positive 
    CHECK (trade_count >= 0);
    
ALTER TABLE positions ADD CONSTRAINT positions_side_valid 
    CHECK (side IS NULL OR side IN ('LONG', 'SHORT'));
    
ALTER TABLE positions ADD CONSTRAINT positions_pending_quantity_positive 
    CHECK (pending_quantity >= 0);

-- Ensure all required indexes exist for performance
CREATE INDEX IF NOT EXISTS idx_positions_user_id ON positions(user_id);
CREATE INDEX IF NOT EXISTS idx_positions_symbol ON positions(symbol);
CREATE INDEX IF NOT EXISTS idx_positions_user_symbol ON positions(user_id, symbol);
CREATE INDEX IF NOT EXISTS idx_positions_updated_at ON positions(updated_at);
CREATE INDEX IF NOT EXISTS idx_positions_market_value ON positions(market_value);
CREATE INDEX IF NOT EXISTS idx_positions_unrealized_pnl ON positions(unrealized_pnl);
CREATE INDEX IF NOT EXISTS idx_positions_active ON positions(user_id, quantity) WHERE quantity != 0;

-- Ensure unique constraint exists (should already exist from previous migration)
-- But let's make sure it's there
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'positions_user_symbol_exchange_unique'
    ) THEN
        ALTER TABLE positions ADD CONSTRAINT positions_user_symbol_exchange_unique 
            UNIQUE(user_id, symbol, exchange);
    END IF;
END $$;

-- Add trigger functions if they don't exist
CREATE OR REPLACE FUNCTION update_positions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Ensure trigger exists
DROP TRIGGER IF EXISTS trg_positions_updated_at ON positions;
CREATE TRIGGER trg_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW
    EXECUTE FUNCTION update_positions_updated_at();

-- Add audit trigger if audit function exists (from V1 migration)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'create_audit_log_entry') THEN
        DROP TRIGGER IF EXISTS trg_positions_audit ON positions;
        CREATE TRIGGER trg_positions_audit
            AFTER INSERT OR UPDATE OR DELETE ON positions
            FOR EACH ROW
            EXECUTE FUNCTION create_audit_log_entry();
    END IF;
END $$;

-- Add comments for all columns to match entity documentation
COMMENT ON TABLE positions IS 'Detailed position tracking with comprehensive P&L and risk metrics matching Position entity';
COMMENT ON COLUMN positions.id IS 'Primary key - auto-generated';
COMMENT ON COLUMN positions.user_id IS 'User who owns this position (NOT NULL)';
COMMENT ON COLUMN positions.symbol IS 'Trading symbol (e.g., RELIANCE, TCS, INFY) - max 20 chars (NOT NULL)';
COMMENT ON COLUMN positions.exchange IS 'Exchange where security is traded (NSE, BSE) - max 10 chars (NOT NULL)';
COMMENT ON COLUMN positions.quantity IS 'Current position quantity (positive = long, negative = short) (NOT NULL, default 0)';
COMMENT ON COLUMN positions.side IS 'Position side derived from quantity (LONG/SHORT) - max 10 chars';
COMMENT ON COLUMN positions.average_cost IS 'Average cost basis per share - NUMERIC(15,4)';
COMMENT ON COLUMN positions.cost_basis IS 'Total cost basis (quantity * average cost) - NUMERIC(20,4)';
COMMENT ON COLUMN positions.current_price IS 'Current market price per share - NUMERIC(15,4)';
COMMENT ON COLUMN positions.market_value IS 'Current market value of position - NUMERIC(20,4)';
COMMENT ON COLUMN positions.unrealized_pnl IS 'Unrealized P&L (market value - cost basis) - NUMERIC(20,4)';
COMMENT ON COLUMN positions.unrealized_pnl_percent IS 'Unrealized P&L percentage - NUMERIC(8,4)';
COMMENT ON COLUMN positions.realized_pnl IS 'Realized P&L from closed positions - NUMERIC(20,4) (NOT NULL, default 0)';
COMMENT ON COLUMN positions.total_pnl IS 'Total P&L (realized + unrealized) - NUMERIC(20,4)';
COMMENT ON COLUMN positions.intraday_pnl IS 'Intraday P&L (change since market open) - NUMERIC(20,4)';
COMMENT ON COLUMN positions.previous_close_value IS 'Previous day closing position value - NUMERIC(20,4)';
COMMENT ON COLUMN positions.day_change IS 'Day change in position value - NUMERIC(20,4)';
COMMENT ON COLUMN positions.day_change_percent IS 'Day change percentage - NUMERIC(8,4)';
COMMENT ON COLUMN positions.pending_quantity IS 'Pending quantity from unexecuted orders (NOT NULL, default 0)';
COMMENT ON COLUMN positions.available_quantity IS 'Available quantity (quantity - pending_quantity)';
COMMENT ON COLUMN positions.max_position_size IS 'Maximum position size held during the day';
COMMENT ON COLUMN positions.min_position_size IS 'Minimum position size held during the day';
COMMENT ON COLUMN positions.margin_requirement IS 'Margin requirement for this position - NUMERIC(15,4)';
COMMENT ON COLUMN positions.margin_utilization IS 'Margin utilization percentage - NUMERIC(5,2)';
COMMENT ON COLUMN positions.borrowing_cost IS 'Borrowing cost for short positions or margin - NUMERIC(15,4) (NOT NULL, default 0)';
COMMENT ON COLUMN positions.risk_score IS 'Position risk score (0.0-1.0) - NUMERIC(5,4)';
COMMENT ON COLUMN positions.portfolio_weight IS 'Position weight in portfolio (percentage) - NUMERIC(5,2)';
COMMENT ON COLUMN positions.beta IS 'Beta to market benchmark - NUMERIC(8,4)';
COMMENT ON COLUMN positions.trade_count IS 'Number of trades that built this position (NOT NULL, default 0)';
COMMENT ON COLUMN positions.first_trade_date IS 'First trade date for this position';
COMMENT ON COLUMN positions.last_trade_date IS 'Last trade date for this position';
COMMENT ON COLUMN positions.days_held IS 'Days held in position';
COMMENT ON COLUMN positions.sector IS 'Sector classification - max 50 chars';
COMMENT ON COLUMN positions.industry IS 'Industry classification - max 50 chars';
COMMENT ON COLUMN positions.asset_class IS 'Asset class (EQUITY, BOND, COMMODITY, etc.) - max 20 chars';
COMMENT ON COLUMN positions.tags IS 'Position tags for categorization - max 200 chars';
COMMENT ON COLUMN positions.created_at IS 'Position creation timestamp (NOT NULL, auto-set on insert)';
COMMENT ON COLUMN positions.updated_at IS 'Last modification timestamp (NOT NULL, auto-updated on change)';
COMMENT ON COLUMN positions.price_updated_at IS 'Last price update timestamp';
COMMENT ON COLUMN positions.metadata IS 'Additional position metadata as JSON TEXT';

-- Final validation: Check that all expected columns exist
DO $$
DECLARE
    missing_columns TEXT[] := ARRAY[]::TEXT[];
    expected_columns TEXT[] := ARRAY[
        'id', 'user_id', 'symbol', 'exchange', 'quantity', 'side',
        'average_cost', 'cost_basis', 'current_price', 'market_value',
        'unrealized_pnl', 'unrealized_pnl_percent', 'realized_pnl', 'total_pnl',
        'intraday_pnl', 'previous_close_value', 'day_change', 'day_change_percent',
        'pending_quantity', 'available_quantity', 'max_position_size', 'min_position_size',
        'margin_requirement', 'margin_utilization', 'borrowing_cost', 'risk_score',
        'portfolio_weight', 'beta', 'trade_count', 'first_trade_date',
        'last_trade_date', 'days_held', 'sector', 'industry',
        'asset_class', 'tags', 'created_at', 'updated_at',
        'price_updated_at', 'metadata'
    ];
    col TEXT;
    exists_check INTEGER;
BEGIN
    FOREACH col IN ARRAY expected_columns LOOP
        SELECT COUNT(*) INTO exists_check
        FROM information_schema.columns 
        WHERE table_name = 'positions' AND column_name = col;
        
        IF exists_check = 0 THEN
            missing_columns := array_append(missing_columns, col);
        END IF;
    END LOOP;
    
    IF array_length(missing_columns, 1) > 0 THEN
        RAISE EXCEPTION 'Missing columns in positions table: %', array_to_string(missing_columns, ', ');
    ELSE
        RAISE NOTICE 'SUCCESS: All % expected columns exist in positions table', array_length(expected_columns, 1);
    END IF;
END $$;