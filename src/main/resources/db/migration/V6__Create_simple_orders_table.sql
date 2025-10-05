-- V6__Create_simple_orders_table.sql
-- Create simple_orders table to match SimpleOrder entity

-- Create simple_orders table
CREATE TABLE IF NOT EXISTS simple_orders (
    id                  BIGSERIAL PRIMARY KEY,
    order_id           VARCHAR(255) UNIQUE NOT NULL,
    user_id            BIGINT NOT NULL,
    symbol             VARCHAR(255) NOT NULL,
    side               VARCHAR(255) NOT NULL,
    quantity           INTEGER NOT NULL,
    price              NUMERIC(10,2),
    order_type         VARCHAR(255) NOT NULL,
    status             VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP,
    broker_order_id    VARCHAR(255)
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_simple_orders_user_id ON simple_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_simple_orders_symbol ON simple_orders(symbol);
CREATE INDEX IF NOT EXISTS idx_simple_orders_status ON simple_orders(status);
CREATE INDEX IF NOT EXISTS idx_simple_orders_created_at ON simple_orders(created_at);
CREATE INDEX IF NOT EXISTS idx_simple_orders_broker_order_id ON simple_orders(broker_order_id) WHERE broker_order_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE simple_orders IS 'Simplified order entity for core trading functionality';
COMMENT ON COLUMN simple_orders.order_id IS 'Unique order identifier';
COMMENT ON COLUMN simple_orders.user_id IS 'User who placed the order';
COMMENT ON COLUMN simple_orders.symbol IS 'Trading symbol';
COMMENT ON COLUMN simple_orders.side IS 'Order side: BUY or SELL';
COMMENT ON COLUMN simple_orders.quantity IS 'Order quantity (must be positive)';
COMMENT ON COLUMN simple_orders.price IS 'Order price (required for LIMIT orders)';
COMMENT ON COLUMN simple_orders.order_type IS 'Order type: MARKET or LIMIT';
COMMENT ON COLUMN simple_orders.status IS 'Order status: PENDING, FILLED, or CANCELLED';
COMMENT ON COLUMN simple_orders.created_at IS 'Order creation timestamp';
COMMENT ON COLUMN simple_orders.updated_at IS 'Last modification timestamp';
COMMENT ON COLUMN simple_orders.broker_order_id IS 'Broker''s internal order ID';