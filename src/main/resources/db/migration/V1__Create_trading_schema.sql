-- Trading Service Database Schema
-- Version: 1.0.0
-- Description: Core trading tables for order management, execution, and portfolio tracking

-- Enable UUID extension for PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Orders table - Core order management
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(10) NOT NULL,
    order_type VARCHAR(20) NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT', 'STOP_LOSS', 'STOP_LIMIT')),
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    limit_price DECIMAL(15,4),
    stop_price DECIMAL(15,4),
    time_in_force VARCHAR(10) NOT NULL DEFAULT 'DAY' CHECK (time_in_force IN ('DAY', 'GTC', 'IOC', 'FOK', 'GTD')),
    expiry_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VALIDATED', 'SUBMITTED', 'ACKNOWLEDGED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'EXPIRED')),
    broker_order_id VARCHAR(100),
    broker_name VARCHAR(50),
    filled_quantity INTEGER DEFAULT 0 CHECK (filled_quantity >= 0),
    avg_fill_price DECIMAL(15,4),
    rejection_reason TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    submitted_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT chk_filled_quantity_lte_quantity CHECK (filled_quantity <= quantity),
    CONSTRAINT chk_limit_price_positive CHECK (limit_price IS NULL OR limit_price > 0),
    CONSTRAINT chk_stop_price_positive CHECK (stop_price IS NULL OR stop_price > 0),
    CONSTRAINT chk_avg_fill_price_positive CHECK (avg_fill_price IS NULL OR avg_fill_price > 0),
    CONSTRAINT chk_limit_required_for_limit_orders CHECK (
        (order_type IN ('LIMIT', 'STOP_LIMIT') AND limit_price IS NOT NULL) OR 
        (order_type NOT IN ('LIMIT', 'STOP_LIMIT'))
    ),
    CONSTRAINT chk_stop_required_for_stop_orders CHECK (
        (order_type IN ('STOP_LOSS', 'STOP_LIMIT') AND stop_price IS NOT NULL) OR 
        (order_type NOT IN ('STOP_LOSS', 'STOP_LIMIT'))
    ),
    CONSTRAINT chk_expiry_date_for_gtd CHECK (
        (time_in_force = 'GTD' AND expiry_date IS NOT NULL) OR 
        (time_in_force != 'GTD')
    )
);

-- Order fills table - Track partial fills and execution details
CREATE TABLE order_fills (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    fill_quantity INTEGER NOT NULL CHECK (fill_quantity > 0),
    fill_price DECIMAL(15,4) NOT NULL CHECK (fill_price > 0),
    fill_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    broker_fill_id VARCHAR(100),
    commission DECIMAL(10,4) DEFAULT 0 CHECK (commission >= 0),
    taxes DECIMAL(10,4) DEFAULT 0 CHECK (taxes >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Trades table - Completed trade records for reporting
CREATE TABLE trades (
    id BIGSERIAL PRIMARY KEY,
    trade_id VARCHAR(50) UNIQUE NOT NULL DEFAULT 'TR-' || EXTRACT(EPOCH FROM NOW()) || '-' || SUBSTR(uuid_generate_v4()::text, 1, 8),
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(10) NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(15,4) NOT NULL CHECK (price > 0),
    trade_value DECIMAL(20,4) NOT NULL CHECK (trade_value > 0),
    commission DECIMAL(10,4) DEFAULT 0 CHECK (commission >= 0),
    taxes DECIMAL(10,4) DEFAULT 0 CHECK (taxes >= 0),
    net_amount DECIMAL(20,4) NOT NULL,
    trade_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    settlement_date DATE,
    broker_trade_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Portfolios table - Current position holdings
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    avg_price DECIMAL(15,4) NOT NULL CHECK (avg_price > 0),
    market_value DECIMAL(20,4),
    unrealized_pnl DECIMAL(20,4),
    realized_pnl DECIMAL(20,4) DEFAULT 0,
    last_price DECIMAL(15,4),
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint on user_id + symbol + exchange
    UNIQUE(user_id, symbol, exchange)
);

-- Portfolio history table - Track portfolio changes over time
CREATE TABLE portfolio_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    avg_price DECIMAL(15,4) NOT NULL,
    market_value DECIMAL(20,4),
    unrealized_pnl DECIMAL(20,4),
    snapshot_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Unique constraint on user_id + symbol + exchange + snapshot_date
    UNIQUE(user_id, symbol, exchange, snapshot_date)
);

-- Risk limits table - User-specific risk parameters
CREATE TABLE risk_limits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    max_position_value DECIMAL(20,4) DEFAULT 10000000, -- ₹1 Crore default
    max_single_order_value DECIMAL(20,4) DEFAULT 1000000, -- ₹10 Lakh default
    max_daily_trades INTEGER DEFAULT 500,
    max_open_orders INTEGER DEFAULT 1000,
    pattern_day_trader BOOLEAN DEFAULT FALSE,
    day_trading_buying_power DECIMAL(20,4) DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit log table - Complete audit trail for compliance
CREATE TABLE trading_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    trade_id BIGINT,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(20) NOT NULL CHECK (entity_type IN ('ORDER', 'TRADE', 'PORTFOLIO', 'RISK')),
    entity_id BIGINT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Performance indexes for high-frequency trading operations

-- Orders table indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_user_symbol_status ON orders(user_id, symbol, status);
CREATE INDEX idx_orders_broker_order_id ON orders(broker_order_id) WHERE broker_order_id IS NOT NULL;
CREATE INDEX idx_orders_active ON orders(user_id, status) WHERE status IN ('ACKNOWLEDGED', 'PARTIALLY_FILLED');

-- Order fills table indexes
CREATE INDEX idx_order_fills_order_id ON order_fills(order_id);
CREATE INDEX idx_order_fills_fill_time ON order_fills(fill_time);

-- Trades table indexes
CREATE INDEX idx_trades_user_id ON trades(user_id);
CREATE INDEX idx_trades_symbol ON trades(symbol);
CREATE INDEX idx_trades_trade_time ON trades(trade_time);
CREATE INDEX idx_trades_user_symbol_time ON trades(user_id, symbol, trade_time);

-- Portfolios table indexes
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_portfolios_symbol ON portfolios(symbol);
CREATE INDEX idx_portfolios_last_updated ON portfolios(last_updated);

-- Portfolio history table indexes
CREATE INDEX idx_portfolio_history_user_date ON portfolio_history(user_id, snapshot_date);
CREATE INDEX idx_portfolio_history_symbol_date ON portfolio_history(symbol, snapshot_date);

-- Risk limits table indexes
CREATE INDEX idx_risk_limits_user_id ON risk_limits(user_id);

-- Audit log table indexes
CREATE INDEX idx_audit_log_user_id ON trading_audit_log(user_id);
CREATE INDEX idx_audit_log_entity ON trading_audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON trading_audit_log(created_at);

-- Functions and triggers for automatic updates

-- Function to update portfolio on trade execution
CREATE OR REPLACE FUNCTION update_portfolio_on_trade()
RETURNS TRIGGER AS $$
BEGIN
    -- Insert or update portfolio position
    INSERT INTO portfolios (user_id, symbol, exchange, quantity, avg_price, last_updated)
    VALUES (NEW.user_id, NEW.symbol, NEW.exchange, 
            CASE WHEN NEW.side = 'BUY' THEN NEW.quantity ELSE -NEW.quantity END,
            NEW.price, NEW.created_at)
    ON CONFLICT (user_id, symbol, exchange)
    DO UPDATE SET
        quantity = portfolios.quantity + CASE WHEN NEW.side = 'BUY' THEN NEW.quantity ELSE -NEW.quantity END,
        avg_price = CASE 
            WHEN NEW.side = 'BUY' THEN 
                (portfolios.avg_price * portfolios.quantity + NEW.price * NEW.quantity) / 
                (portfolios.quantity + NEW.quantity)
            ELSE portfolios.avg_price
        END,
        last_updated = NEW.created_at;
        
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update portfolio on trade insertion
CREATE TRIGGER trg_update_portfolio_on_trade
    AFTER INSERT ON trades
    FOR EACH ROW
    EXECUTE FUNCTION update_portfolio_on_trade();

-- Function to create audit log entries
CREATE OR REPLACE FUNCTION create_audit_log_entry()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO trading_audit_log (
        user_id, order_id, action, entity_type, entity_id, 
        old_values, new_values, created_at
    )
    VALUES (
        COALESCE(NEW.user_id, OLD.user_id),
        CASE WHEN TG_TABLE_NAME = 'orders' THEN COALESCE(NEW.id, OLD.id) ELSE NULL END,
        TG_OP,
        UPPER(TG_TABLE_NAME),
        COALESCE(NEW.id, OLD.id),
        CASE WHEN TG_OP = 'DELETE' THEN row_to_json(OLD) ELSE NULL END,
        CASE WHEN TG_OP != 'DELETE' THEN row_to_json(NEW) ELSE NULL END,
        NOW()
    );
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Audit triggers for all trading tables
CREATE TRIGGER trg_orders_audit
    AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION create_audit_log_entry();

CREATE TRIGGER trg_trades_audit
    AFTER INSERT OR UPDATE OR DELETE ON trades
    FOR EACH ROW
    EXECUTE FUNCTION create_audit_log_entry();

CREATE TRIGGER trg_portfolios_audit
    AFTER INSERT OR UPDATE OR DELETE ON portfolios
    FOR EACH ROW
    EXECUTE FUNCTION create_audit_log_entry();

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for updating updated_at timestamp
CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_risk_limits_updated_at
    BEFORE UPDATE ON risk_limits
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default risk limits for system testing
INSERT INTO risk_limits (user_id, max_position_value, max_single_order_value, max_daily_trades)
VALUES 
    (1, 5000000, 500000, 100),  -- Conservative limits for user 1
    (2, 10000000, 1000000, 500); -- Standard limits for user 2

-- Comments for documentation
COMMENT ON TABLE orders IS 'Core order management table with lifecycle tracking';
COMMENT ON TABLE order_fills IS 'Partial fill tracking for order execution';
COMMENT ON TABLE trades IS 'Completed trade records for reporting and settlement';
COMMENT ON TABLE portfolios IS 'Current position holdings with P&L calculations';
COMMENT ON TABLE portfolio_history IS 'Historical portfolio snapshots for performance tracking';
COMMENT ON TABLE risk_limits IS 'User-specific risk management parameters';
COMMENT ON TABLE trading_audit_log IS 'Complete audit trail for regulatory compliance';

COMMENT ON COLUMN orders.order_id IS 'Unique external order identifier (TM-timestamp-random)';
COMMENT ON COLUMN orders.time_in_force IS 'Order validity period (DAY, GTC, IOC, FOK, GTD)';
COMMENT ON COLUMN orders.metadata IS 'Additional order attributes as JSON';
COMMENT ON COLUMN portfolios.unrealized_pnl IS 'Mark-to-market profit/loss';
COMMENT ON COLUMN portfolios.realized_pnl IS 'Actual profit/loss from closed positions';