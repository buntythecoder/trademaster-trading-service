-- V5__Add_missing_risk_limits_columns.sql
-- Add all missing columns to risk_limits table to match RiskLimit entity

-- Check if columns exist before adding them to make migration idempotent
DO $$
BEGIN
    -- Add profile_type column
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'profile_type') THEN
        ALTER TABLE risk_limits ADD COLUMN profile_type VARCHAR(20);
    END IF;

    -- Add Position Limit columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_single_position_value') THEN
        ALTER TABLE risk_limits ADD COLUMN max_single_position_value NUMERIC(15,2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_single_position_percent') THEN
        ALTER TABLE risk_limits ADD COLUMN max_single_position_percent NUMERIC(5,2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_sector_concentration') THEN
        ALTER TABLE risk_limits ADD COLUMN max_sector_concentration NUMERIC(5,2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_total_positions') THEN
        ALTER TABLE risk_limits ADD COLUMN max_total_positions INTEGER;
    END IF;

    -- Add Leverage Limit columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_leverage_ratio') THEN
        ALTER TABLE risk_limits ADD COLUMN max_leverage_ratio NUMERIC(5,2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_margin_utilization') THEN
        ALTER TABLE risk_limits ADD COLUMN max_margin_utilization NUMERIC(5,2);
    END IF;

    -- Add Trading Velocity Limit columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_daily_trading_value') THEN
        ALTER TABLE risk_limits ADD COLUMN max_daily_trading_value NUMERIC(15,2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_daily_orders') THEN
        ALTER TABLE risk_limits ADD COLUMN max_daily_orders INTEGER;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_orders_per_minute') THEN
        ALTER TABLE risk_limits ADD COLUMN max_orders_per_minute INTEGER;
    END IF;

    -- Add Portfolio Risk Limit columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_var') THEN
        ALTER TABLE risk_limits ADD COLUMN max_var NUMERIC(15,2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_drawdown') THEN
        ALTER TABLE risk_limits ADD COLUMN max_drawdown NUMERIC(5,2);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'max_volatility') THEN
        ALTER TABLE risk_limits ADD COLUMN max_volatility NUMERIC(5,4);
    END IF;

    -- Add Dynamic Adjustment columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'enable_volatility_adjustment') THEN
        ALTER TABLE risk_limits ADD COLUMN enable_volatility_adjustment BOOLEAN NOT NULL DEFAULT false;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'volatility_multiplier') THEN
        ALTER TABLE risk_limits ADD COLUMN volatility_multiplier NUMERIC(5,3) DEFAULT 1.0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'enable_market_regime_adjustment') THEN
        ALTER TABLE risk_limits ADD COLUMN enable_market_regime_adjustment BOOLEAN NOT NULL DEFAULT false;
    END IF;

    -- Add Alert Configuration columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'warning_threshold_percent') THEN
        ALTER TABLE risk_limits ADD COLUMN warning_threshold_percent NUMERIC(5,2) DEFAULT 80.0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'critical_threshold_percent') THEN
        ALTER TABLE risk_limits ADD COLUMN critical_threshold_percent NUMERIC(5,2) DEFAULT 95.0;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'enable_real_time_alerts') THEN
        ALTER TABLE risk_limits ADD COLUMN enable_real_time_alerts BOOLEAN NOT NULL DEFAULT true;
    END IF;

    -- Add Audit columns (the ones causing the failure)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'created_by') THEN
        ALTER TABLE risk_limits ADD COLUMN created_by VARCHAR(50);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'updated_by') THEN
        ALTER TABLE risk_limits ADD COLUMN updated_by VARCHAR(50);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'risk_limits' AND column_name = 'version') THEN
        ALTER TABLE risk_limits ADD COLUMN version BIGINT;
    END IF;
END $$;

-- Add missing index for profile_type (as specified in the entity)
CREATE INDEX IF NOT EXISTS idx_risk_limits_profile_type ON risk_limits(profile_type);

-- Add constraints for business logic
DO $$
BEGIN
    -- Constraint for positive values
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_risk_limits_positive_values') THEN
        ALTER TABLE risk_limits ADD CONSTRAINT chk_risk_limits_positive_values 
            CHECK (
                (max_single_position_value IS NULL OR max_single_position_value > 0) AND
                (max_single_position_percent IS NULL OR max_single_position_percent > 0) AND
                (max_sector_concentration IS NULL OR max_sector_concentration > 0) AND
                (max_total_positions IS NULL OR max_total_positions > 0) AND
                (max_leverage_ratio IS NULL OR max_leverage_ratio > 0) AND
                (max_margin_utilization IS NULL OR max_margin_utilization > 0) AND
                (max_daily_trading_value IS NULL OR max_daily_trading_value > 0) AND
                (max_daily_orders IS NULL OR max_daily_orders > 0) AND
                (max_orders_per_minute IS NULL OR max_orders_per_minute > 0) AND
                (max_var IS NULL OR max_var > 0) AND
                (max_drawdown IS NULL OR max_drawdown > 0) AND
                (max_volatility IS NULL OR max_volatility > 0) AND
                (volatility_multiplier IS NULL OR volatility_multiplier > 0)
            );
    END IF;

    -- Constraint for percentage values (0-100)
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_risk_limits_percentage_values') THEN
        ALTER TABLE risk_limits ADD CONSTRAINT chk_risk_limits_percentage_values
            CHECK (
                (max_single_position_percent IS NULL OR (max_single_position_percent >= 0 AND max_single_position_percent <= 100)) AND
                (max_sector_concentration IS NULL OR (max_sector_concentration >= 0 AND max_sector_concentration <= 100)) AND
                (max_margin_utilization IS NULL OR (max_margin_utilization >= 0 AND max_margin_utilization <= 100)) AND
                (max_drawdown IS NULL OR (max_drawdown >= 0 AND max_drawdown <= 100)) AND
                (warning_threshold_percent IS NULL OR (warning_threshold_percent >= 0 AND warning_threshold_percent <= 100)) AND
                (critical_threshold_percent IS NULL OR (critical_threshold_percent >= 0 AND critical_threshold_percent <= 100))
            );
    END IF;

    -- Constraint for profile type values
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_risk_limits_profile_type') THEN
        ALTER TABLE risk_limits ADD CONSTRAINT chk_risk_limits_profile_type
            CHECK (profile_type IS NULL OR profile_type IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE', 'CUSTOM'));
    END IF;

    -- Constraint for threshold ordering
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_risk_limits_threshold_ordering') THEN
        ALTER TABLE risk_limits ADD CONSTRAINT chk_risk_limits_threshold_ordering
            CHECK (warning_threshold_percent IS NULL OR critical_threshold_percent IS NULL OR 
                   warning_threshold_percent <= critical_threshold_percent);
    END IF;
END $$;

-- Update existing records to set defaults for new NOT NULL columns
UPDATE risk_limits 
SET 
    enable_volatility_adjustment = COALESCE(enable_volatility_adjustment, false),
    enable_market_regime_adjustment = COALESCE(enable_market_regime_adjustment, false),
    enable_real_time_alerts = COALESCE(enable_real_time_alerts, true),
    volatility_multiplier = COALESCE(volatility_multiplier, 1.0),
    warning_threshold_percent = COALESCE(warning_threshold_percent, 80.0),
    critical_threshold_percent = COALESCE(critical_threshold_percent, 95.0)
WHERE 
    enable_volatility_adjustment IS NULL OR 
    enable_market_regime_adjustment IS NULL OR 
    enable_real_time_alerts IS NULL OR
    volatility_multiplier IS NULL OR
    warning_threshold_percent IS NULL OR
    critical_threshold_percent IS NULL;

-- Add comments for documentation
COMMENT ON TABLE risk_limits IS 'User risk limits and trading restrictions with dynamic adjustments';
COMMENT ON COLUMN risk_limits.profile_type IS 'Risk profile type: CONSERVATIVE, MODERATE, AGGRESSIVE, CUSTOM';
COMMENT ON COLUMN risk_limits.max_single_position_value IS 'Maximum value for a single position';
COMMENT ON COLUMN risk_limits.max_single_position_percent IS 'Maximum percentage of portfolio for a single position';
COMMENT ON COLUMN risk_limits.max_sector_concentration IS 'Maximum portfolio concentration in a single sector';
COMMENT ON COLUMN risk_limits.max_leverage_ratio IS 'Maximum leverage ratio allowed';
COMMENT ON COLUMN risk_limits.enable_volatility_adjustment IS 'Enable dynamic adjustments based on market volatility';
COMMENT ON COLUMN risk_limits.volatility_multiplier IS 'Multiplier for limits during high volatility periods';
COMMENT ON COLUMN risk_limits.warning_threshold_percent IS 'Threshold percentage to trigger warning alerts';
COMMENT ON COLUMN risk_limits.critical_threshold_percent IS 'Threshold percentage to trigger critical alerts';
COMMENT ON COLUMN risk_limits.created_by IS 'User who created this risk limit configuration';
COMMENT ON COLUMN risk_limits.updated_by IS 'User who last updated this risk limit configuration';
COMMENT ON COLUMN risk_limits.version IS 'Optimistic locking version number';

-- Final validation: Check that all expected columns exist
DO $$
DECLARE
    missing_columns TEXT[] := ARRAY[]::TEXT[];
    expected_columns TEXT[] := ARRAY[
        'id', 'user_id', 'profile_type', 'active', 'max_single_position_value',
        'max_single_position_percent', 'max_sector_concentration', 'max_total_positions',
        'max_leverage_ratio', 'max_margin_utilization', 'allow_margin_trading',
        'max_daily_trading_value', 'max_daily_orders', 'max_orders_per_minute',
        'max_var', 'max_drawdown', 'max_volatility', 'enable_volatility_adjustment',
        'volatility_multiplier', 'enable_market_regime_adjustment',
        'warning_threshold_percent', 'critical_threshold_percent', 'enable_real_time_alerts',
        'created_at', 'updated_at', 'created_by', 'updated_by', 'version'
    ];
    col TEXT;
    exists_check INTEGER;
BEGIN
    FOREACH col IN ARRAY expected_columns LOOP
        SELECT COUNT(*) INTO exists_check
        FROM information_schema.columns 
        WHERE table_name = 'risk_limits' AND column_name = col;
        
        IF exists_check = 0 THEN
            missing_columns := array_append(missing_columns, col);
        END IF;
    END LOOP;
    
    IF array_length(missing_columns, 1) > 0 THEN
        RAISE EXCEPTION 'Missing columns in risk_limits table: %', array_to_string(missing_columns, ', ');
    ELSE
        RAISE NOTICE 'SUCCESS: All % expected columns exist in risk_limits table', array_length(expected_columns, 1);
    END IF;
END $$;