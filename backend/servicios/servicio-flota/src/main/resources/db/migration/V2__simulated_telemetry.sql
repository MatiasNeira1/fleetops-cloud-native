ALTER TABLE vehicle_positions ADD COLUMN source VARCHAR(80);
ALTER TABLE telemetry_events ADD COLUMN source VARCHAR(80);
CREATE INDEX idx_alerts_open_condition ON alerts(vehicle_id, type, status);
