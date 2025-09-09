-- V3__Create_positions_table.sql
-- Create positions table to match Position entity requirements
-- This is separate from portfolios table which serves a different purpose

-- Positions table - Detailed position tracking with comprehensive metrics
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    
    -- Core Position Information
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    side VARCHAR(10),
    
    -- Cost Basis Tracking
    average_cost NUMERIC(15,4),
    cost_basis NUMERIC(20,4),
    
    -- Market Value & P&L
    current_price NUMERIC(15,4),
    market_value NUMERIC(20,4),
    unrealized_pnl NUMERIC(20,4),
    unrealized_pnl_percent NUMERIC(8,4),
    realized_pnl NUMERIC(20,4) DEFAULT 0,
    total_pnl NUMERIC(20,4),
    
    -- Intraday Metrics
    intraday_pnl NUMERIC(20,4),
    previous_close_value NUMERIC(20,4),
    day_change NUMERIC(20,4),
    day_change_percent NUMERIC(8,4),
    
    -- Position Management
    pending_quantity INTEGER DEFAULT 0,
    available_quantity INTEGER,
    max_position_size INTEGER,
    min_position_size INTEGER,
    
    -- Risk & Margin
    margin_requirement NUMERIC(15,4),
    margin_utilization NUMERIC(5,2),
    borrowing_cost NUMERIC(15,4) DEFAULT 0,
    risk_score NUMERIC(5,4),
    portfolio_weight NUMERIC(5,2),
    beta NUMERIC(8,4),
    
    -- Trade Tracking
    trade_count INTEGER DEFAULT 0,
    first_trade_date DATE,
    last_trade_date DATE,
    days_held INTEGER,
    
    -- Classification
    sector VARCHAR(50),
    industry VARCHAR(50),
    asset_class VARCHAR(20),
    tags VARCHAR(200),
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    price_updated_at TIMESTAMP WITH TIME ZONE,
    
    -- Metadata
    metadata TEXT,
    
    -- Constraints
    CONSTRAINT positions_user_symbol_exchange_unique UNIQUE(user_id, symbol, exchange),
    CONSTRAINT positions_margin_requirement_positive CHECK (margin_requirement IS NULL OR margin_requirement >= 0),
    CONSTRAINT positions_margin_utilization_valid CHECK (margin_utilization IS NULL OR (margin_utilization >= 0 AND margin_utilization <= 100)),
    CONSTRAINT positions_risk_score_valid CHECK (risk_score IS NULL OR (risk_score >= 0 AND risk_score <= 1)),
    CONSTRAINT positions_portfolio_weight_valid CHECK (portfolio_weight IS NULL OR (portfolio_weight >= 0 AND portfolio_weight <= 100)),
    CONSTRAINT positions_trade_count_positive CHECK (trade_count >= 0),
    CONSTRAINT positions_side_valid CHECK (side IS NULL OR side IN ('LONG', 'SHORT')),
    CONSTRAINT positions_pending_quantity_positive CHECK (pending_quantity >= 0)
);

-- Performance indexes for positions table
CREATE INDEX idx_positions_user_id ON positions(user_id);
CREATE INDEX idx_positions_symbol ON positions(symbol);
CREATE INDEX idx_positions_user_symbol ON positions(user_id, symbol);
CREATE INDEX idx_positions_updated_at ON positions(updated_at);
CREATE INDEX idx_positions_market_value ON positions(market_value);
CREATE INDEX idx_positions_unrealized_pnl ON positions(unrealized_pnl);
CREATE INDEX idx_positions_active ON positions(user_id, quantity) WHERE quantity != 0;

-- Function to update positions updated_at timestamp
CREATE OR REPLACE FUNCTION update_positions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updating updated_at timestamp on positions
CREATE TRIGGER trg_positions_updated_at
    BEFORE UPDATE ON positions
    FOR EACH ROW
    EXECUTE FUNCTION update_positions_updated_at();

-- Audit trigger for positions table
CREATE TRIGGER trg_positions_audit
    AFTER INSERT OR UPDATE OR DELETE ON positions
    FOR EACH ROW
    EXECUTE FUNCTION create_audit_log_entry();

-- Comments for documentation
COMMENT ON TABLE positions IS 'Detailed position tracking with comprehensive P&L and risk metrics';
COMMENT ON COLUMN positions.user_id IS 'User who owns this position';
COMMENT ON COLUMN positions.symbol IS 'Trading symbol (e.g., RELIANCE, TCS, INFY)';
COMMENT ON COLUMN positions.exchange IS 'Exchange where security is traded (NSE, BSE)';
COMMENT ON COLUMN positions.quantity IS 'Current position quantity (positive = long, negative = short)';
COMMENT ON COLUMN positions.side IS 'Position side derived from quantity (LONG/SHORT)';
COMMENT ON COLUMN positions.average_cost IS 'Average cost basis per share';
COMMENT ON COLUMN positions.cost_basis IS 'Total cost basis (quantity * average cost)';
COMMENT ON COLUMN positions.current_price IS 'Current market price per share';
COMMENT ON COLUMN positions.market_value IS 'Current market value of position';
COMMENT ON COLUMN positions.unrealized_pnl IS 'Unrealized P&L (market value - cost basis)';
COMMENT ON COLUMN positions.unrealized_pnl_percent IS 'Unrealized P&L percentage';
COMMENT ON COLUMN positions.realized_pnl IS 'Realized P&L from closed positions';
COMMENT ON COLUMN positions.total_pnl IS 'Total P&L (realized + unrealized)';
COMMENT ON COLUMN positions.intraday_pnl IS 'Intraday P&L (change since market open)';
COMMENT ON COLUMN positions.previous_close_value IS 'Previous day closing position value';
COMMENT ON COLUMN positions.day_change IS 'Day change in position value';
COMMENT ON COLUMN positions.day_change_percent IS 'Day change percentage';
COMMENT ON COLUMN positions.pending_quantity IS 'Pending quantity from unexecuted orders';
COMMENT ON COLUMN positions.available_quantity IS 'Available quantity (quantity - pending_quantity)';
COMMENT ON COLUMN positions.max_position_size IS 'Maximum position size held during the day';
COMMENT ON COLUMN positions.min_position_size IS 'Minimum position size held during the day';
COMMENT ON COLUMN positions.margin_requirement IS 'Margin requirement for this position';
COMMENT ON COLUMN positions.margin_utilization IS 'Margin utilization percentage';
COMMENT ON COLUMN positions.borrowing_cost IS 'Borrowing cost for short positions or margin';
COMMENT ON COLUMN positions.risk_score IS 'Position risk score (0.0-1.0)';
COMMENT ON COLUMN positions.portfolio_weight IS 'Position weight in portfolio (percentage)';
COMMENT ON COLUMN positions.beta IS 'Beta to market benchmark';
COMMENT ON COLUMN positions.trade_count IS 'Number of trades that built this position';
COMMENT ON COLUMN positions.first_trade_date IS 'First trade date for this position';
COMMENT ON COLUMN positions.last_trade_date IS 'Last trade date for this position';
COMMENT ON COLUMN positions.days_held IS 'Days held in position';
COMMENT ON COLUMN positions.sector IS 'Sector classification';
COMMENT ON COLUMN positions.industry IS 'Industry classification';
COMMENT ON COLUMN positions.asset_class IS 'Asset class (EQUITY, BOND, COMMODITY, etc.)';
COMMENT ON COLUMN positions.tags IS 'Position tags for categorization';
COMMENT ON COLUMN positions.created_at IS 'Position creation timestamp';
COMMENT ON COLUMN positions.updated_at IS 'Last modification timestamp';
COMMENT ON COLUMN positions.price_updated_at IS 'Last price update timestamp';
COMMENT ON COLUMN positions.metadata IS 'Additional position metadata as JSON';