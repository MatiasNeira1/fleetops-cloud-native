CREATE TABLE alerts.alerts (
    id BIGSERIAL PRIMARY KEY,
    source_event_id UUID NOT NULL UNIQUE,
    source_alert_id BIGINT,
    correlation_id VARCHAR(120) NOT NULL,
    trip_id VARCHAR(80) NOT NULL,
    vehicle_id VARCHAR(80) NOT NULL,
    type VARCHAR(40) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(600) NOT NULL,
    measured_value NUMERIC(14, 3) NOT NULL,
    threshold_value NUMERIC(14, 3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_alerts_trip_id ON alerts.alerts(trip_id);
CREATE INDEX idx_alerts_vehicle_id ON alerts.alerts(vehicle_id);
CREATE INDEX idx_alerts_status ON alerts.alerts(status);
CREATE INDEX idx_alerts_occurred_at ON alerts.alerts(occurred_at);
