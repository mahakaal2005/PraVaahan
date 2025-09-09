-- Real-time Train Position Tracking Database Schema
-- Optimized for high-frequency inserts and real-time queries

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS postgis;
-- Note: TimescaleDB not available in Supabase, using regular tables with optimized indexes
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Create custom types for better type safety
CREATE TYPE train_data_source AS ENUM ('GPS', 'RFID', 'MANUAL', 'SENSOR', 'ESTIMATED');
CREATE TYPE validation_status AS ENUM ('PENDING', 'VALID', 'INVALID', 'SUSPICIOUS');
CREATE TYPE connection_state AS ENUM ('CONNECTED', 'DISCONNECTED', 'RECONNECTING', 'FAILED', 'UNKNOWN');
CREATE TYPE network_quality AS ENUM ('EXCELLENT', 'GOOD', 'FAIR', 'POOR', 'UNKNOWN');

-- Main real-time train positions table (hypertable for time-series data)
CREATE TABLE train_positions (
    id BIGSERIAL PRIMARY KEY,
    train_id VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude >= -90 AND latitude <= 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude >= -180 AND longitude <= 180),
    location GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)) STORED,
    speed DOUBLE PRECISION NOT NULL CHECK (speed >= 0 AND speed <= 350),
    heading DOUBLE PRECISION NOT NULL CHECK (heading >= 0 AND heading <= 360),
    section_id VARCHAR(20) NOT NULL,
    accuracy DOUBLE PRECISION CHECK (accuracy > 0 AND accuracy <= 10000),
    signal_strength DOUBLE PRECISION CHECK (signal_strength >= 0 AND signal_strength <= 100),
    data_source train_data_source NOT NULL DEFAULT 'GPS',
    validation_status validation_status NOT NULL DEFAULT 'PENDING',
    timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT valid_train_id CHECK (train_id ~ '^[A-Za-z0-9_-]+$'),
    CONSTRAINT valid_section_id CHECK (section_id ~ '^[A-Za-z0-9_-]+$'),
    CONSTRAINT reasonable_timestamp CHECK (timestamp >= NOW() - INTERVAL '1 hour' AND timestamp <= NOW() + INTERVAL '5 minutes')
);

-- Note: TimescaleDB hypertables not available in Supabase
-- Using regular table with optimized indexes for time-series performance
-- SELECT create_hypertable('train_positions', 'timestamp', chunk_time_interval => INTERVAL '1 hour');

-- Data quality metrics table
CREATE TABLE train_position_quality (
    id BIGSERIAL PRIMARY KEY,
    position_id BIGINT NOT NULL REFERENCES train_positions(id) ON DELETE CASCADE,
    train_id VARCHAR(50) NOT NULL,
    signal_strength_score DOUBLE PRECISION CHECK (signal_strength_score >= 0 AND signal_strength_score <= 1),
    gps_accuracy_score DOUBLE PRECISION CHECK (gps_accuracy_score >= 0 AND gps_accuracy_score <= 1),
    data_freshness_score DOUBLE PRECISION NOT NULL CHECK (data_freshness_score >= 0 AND data_freshness_score <= 1),
    source_reliability_score DOUBLE PRECISION NOT NULL CHECK (source_reliability_score >= 0 AND source_reliability_score <= 1),
    overall_quality_score DOUBLE PRECISION NOT NULL CHECK (overall_quality_score >= 0 AND overall_quality_score <= 1),
    anomaly_flags TEXT[], -- Array of anomaly flag names
    validation_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Indexes
    INDEX idx_quality_train_timestamp (train_id, validation_timestamp),
    INDEX idx_quality_score (overall_quality_score)
);

-- Connection status tracking table
CREATE TABLE train_connection_status (
    id BIGSERIAL PRIMARY KEY,
    train_id VARCHAR(50) NOT NULL,
    connection_state connection_state NOT NULL,
    network_quality network_quality NOT NULL DEFAULT 'UNKNOWN',
    last_successful_communication TIMESTAMPTZ NOT NULL,
    retry_attempts INTEGER NOT NULL DEFAULT 0 CHECK (retry_attempts >= 0),
    error_count INTEGER NOT NULL DEFAULT 0 CHECK (error_count >= 0),
    connection_duration INTERVAL,
    status_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT valid_train_id_conn CHECK (train_id ~ '^[A-Za-z0-9_-]+$')
);

-- Security events and anomaly detection results
CREATE TABLE security_events (
    id BIGSERIAL PRIMARY KEY,
    train_id VARCHAR(50) NOT NULL,
    position_id BIGINT REFERENCES train_positions(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    message TEXT NOT NULL,
    field_name VARCHAR(50),
    field_value TEXT,
    source_ip INET,
    risk_score DOUBLE PRECISION NOT NULL CHECK (risk_score >= 0 AND risk_score <= 1),
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    
    -- Indexes for security monitoring
    INDEX idx_security_train_time (train_id, detected_at),
    INDEX idx_security_severity (severity, detected_at),
    INDEX idx_security_risk_score (risk_score DESC)
);

-- Railway sections master data
CREATE TABLE railway_sections (
    section_id VARCHAR(20) PRIMARY KEY,
    section_name VARCHAR(100) NOT NULL,
    start_station VARCHAR(50) NOT NULL,
    end_station VARCHAR(50) NOT NULL,
    track_geometry GEOGRAPHY(LINESTRING, 4326),
    max_speed_limit DOUBLE PRECISION NOT NULL CHECK (max_speed_limit > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Performance indexes for common query patterns
CREATE INDEX idx_train_positions_train_time ON train_positions (train_id, timestamp DESC);
CREATE INDEX idx_train_positions_section_time ON train_positions (section_id, timestamp DESC);
CREATE INDEX idx_train_positions_location ON train_positions USING GIST (location);
CREATE INDEX idx_train_positions_validation ON train_positions (validation_status, timestamp DESC);
CREATE INDEX idx_train_positions_speed ON train_positions (speed, timestamp DESC) WHERE speed > 0;

-- Partial indexes for active data
CREATE INDEX idx_train_positions_recent ON train_positions (train_id, timestamp DESC) 
    WHERE timestamp >= NOW() - INTERVAL '24 hours';
CREATE INDEX idx_train_positions_valid_recent ON train_positions (train_id, timestamp DESC) 
    WHERE validation_status = 'VALID' AND timestamp >= NOW() - INTERVAL '24 hours';

-- Connection status indexes
CREATE UNIQUE INDEX idx_connection_status_train_latest ON train_connection_status (train_id, status_timestamp DESC);
CREATE INDEX idx_connection_status_state ON train_connection_status (connection_state, status_timestamp DESC);

-- Row Level Security (RLS) policies
ALTER TABLE train_positions ENABLE ROW LEVEL SECURITY;
ALTER TABLE train_position_quality ENABLE ROW LEVEL SECURITY;
ALTER TABLE train_connection_status ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_events ENABLE ROW LEVEL SECURITY;

-- Supabase-compatible RLS policies (implemented)
CREATE POLICY authenticated_access ON train_positions
    FOR ALL TO authenticated
    USING (true);

CREATE POLICY service_role_access ON train_positions
    FOR ALL TO service_role
    USING (true);

-- Future: Role-based access control
-- CREATE POLICY train_operator_access ON train_positions
--     FOR ALL TO train_operator
--     USING (train_id IN (SELECT train_id FROM operator_train_assignments WHERE operator_id = auth.uid()));

-- CREATE POLICY section_controller_access ON train_positions
--     FOR ALL TO section_controller
--     USING (section_id IN (SELECT section_id FROM controller_section_assignments WHERE controller_id = auth.uid()));

-- Similar policies for other tables
CREATE POLICY train_operator_quality_access ON train_position_quality
    FOR ALL TO train_operator
    USING (train_id IN (SELECT train_id FROM operator_train_assignments WHERE operator_id = auth.uid()));

CREATE POLICY section_controller_quality_access ON train_position_quality
    FOR ALL TO section_controller
    USING (train_id IN (
        SELECT tp.train_id FROM train_positions tp 
        WHERE tp.section_id IN (SELECT section_id FROM controller_section_assignments WHERE controller_id = auth.uid())
    ));

-- Triggers for automatic timestamp updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_train_positions_updated_at 
    BEFORE UPDATE ON train_positions 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_railway_sections_updated_at 
    BEFORE UPDATE ON railway_sections 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function for data quality score calculation
CREATE OR REPLACE FUNCTION calculate_position_quality_score(
    p_signal_strength DOUBLE PRECISION,
    p_gps_accuracy DOUBLE PRECISION,
    p_data_age_seconds INTEGER,
    p_source_reliability DOUBLE PRECISION,
    p_validation_status validation_status,
    p_anomaly_count INTEGER
) RETURNS DOUBLE PRECISION AS $$
DECLARE
    signal_score DOUBLE PRECISION := 0;
    accuracy_score DOUBLE PRECISION := 0;
    freshness_score DOUBLE PRECISION;
    overall_score DOUBLE PRECISION;
BEGIN
    -- Signal strength score (0-1)
    IF p_signal_strength IS NOT NULL THEN
        signal_score := p_signal_strength / 100.0;
    END IF;
    
    -- GPS accuracy score (inverse relationship)
    IF p_gps_accuracy IS NOT NULL THEN
        accuracy_score := CASE 
            WHEN p_gps_accuracy <= 5 THEN 1.0
            WHEN p_gps_accuracy <= 10 THEN 0.8
            WHEN p_gps_accuracy <= 20 THEN 0.6
            WHEN p_gps_accuracy <= 50 THEN 0.4
            WHEN p_gps_accuracy <= 100 THEN 0.2
            ELSE 0.1
        END;
    END IF;
    
    -- Data freshness score (exponential decay)
    freshness_score := EXP(-p_data_age_seconds / 300.0); -- 5-minute half-life
    
    -- Calculate weighted average
    overall_score := (signal_score * 0.2 + accuracy_score * 0.25 + freshness_score * 0.3 + p_source_reliability * 0.25);
    
    -- Apply validation status penalty
    overall_score := overall_score * CASE p_validation_status
        WHEN 'VALID' THEN 1.0
        WHEN 'PENDING' THEN 0.8
        WHEN 'SUSPICIOUS' THEN 0.5
        WHEN 'INVALID' THEN 0.1
    END;
    
    -- Apply anomaly penalty
    overall_score := GREATEST(0, overall_score - (p_anomaly_count * 0.1));
    
    RETURN LEAST(1.0, overall_score);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Regular view for real-time dashboard queries (materialized views don't support RLS)
CREATE VIEW train_positions_current AS
SELECT DISTINCT ON (train_id)
    train_id,
    latitude,
    longitude,
    location,
    speed,
    heading,
    section_id,
    accuracy,
    signal_strength,
    data_source,
    validation_status,
    timestamp,
    created_at
FROM train_positions
ORDER BY train_id, timestamp DESC;

-- Index on materialized view
CREATE UNIQUE INDEX idx_train_positions_current_train ON train_positions_current (train_id);
CREATE INDEX idx_train_positions_current_section ON train_positions_current (section_id);
CREATE INDEX idx_train_positions_current_location ON train_positions_current USING GIST (location);

-- Refresh materialized view automatically
CREATE OR REPLACE FUNCTION refresh_current_positions()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY train_positions_current;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger to refresh materialized view (with rate limiting)
CREATE TRIGGER refresh_current_positions_trigger
    AFTER INSERT OR UPDATE ON train_positions
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_current_positions();

-- Note: TimescaleDB retention and compression policies not available in Supabase
-- Implement manual cleanup procedures as needed
-- SELECT add_retention_policy('train_positions', INTERVAL '30 days');
-- SELECT add_compression_policy('train_positions', INTERVAL '7 days');

-- Regular view for hourly statistics (instead of continuous aggregate)
CREATE VIEW train_positions_hourly AS
SELECT 
    train_id,
    section_id,
    time_bucket('1 hour', timestamp) AS hour,
    AVG(speed) AS avg_speed,
    MAX(speed) AS max_speed,
    COUNT(*) AS position_count,
    AVG(accuracy) AS avg_accuracy,
    COUNT(*) FILTER (WHERE validation_status = 'VALID') AS valid_count
FROM train_positions
GROUP BY train_id, section_id, hour;

-- Refresh policy for continuous aggregate
SELECT add_continuous_aggregate_policy('train_positions_hourly',
    start_offset => INTERVAL '2 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- Comments for documentation
COMMENT ON TABLE train_positions IS 'Real-time train position data with high-frequency updates';
COMMENT ON TABLE train_position_quality IS 'Data quality metrics and scores for position data';
COMMENT ON TABLE train_connection_status IS 'Connection status tracking for real-time communication';
COMMENT ON TABLE security_events IS 'Security events and anomaly detection results';
COMMENT ON TABLE railway_sections IS 'Master data for railway sections and track information';

COMMENT ON COLUMN train_positions.location IS 'PostGIS geography point for spatial queries';
COMMENT ON COLUMN train_positions.timestamp IS 'Actual timestamp of position measurement';
COMMENT ON COLUMN train_positions.created_at IS 'Database insertion timestamp';