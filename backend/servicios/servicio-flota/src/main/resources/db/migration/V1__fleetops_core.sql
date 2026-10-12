CREATE TABLE vehicles (
    id VARCHAR(80) PRIMARY KEY,
    plate VARCHAR(30) NOT NULL
);

CREATE TABLE trips (
    id VARCHAR(80) PRIMARY KEY,
    origin_name VARCHAR(160) NOT NULL,
    origin_latitude NUMERIC(10, 7) NOT NULL,
    origin_longitude NUMERIC(10, 7) NOT NULL,
    destination_name VARCHAR(160) NOT NULL,
    destination_latitude NUMERIC(10, 7) NOT NULL,
    destination_longitude NUMERIC(10, 7) NOT NULL,
    departure_time TIMESTAMPTZ NOT NULL,
    estimated_arrival TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL,
    progress INTEGER NOT NULL,
    total_distance_km NUMERIC(10, 2) NOT NULL,
    travelled_distance_km NUMERIC(10, 2) NOT NULL,
    vehicle_id VARCHAR(80) NOT NULL REFERENCES vehicles(id),
    vehicle_plate VARCHAR(30) NOT NULL,
    driver_id VARCHAR(80) NOT NULL,
    driver_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE vehicle_positions (
    id BIGSERIAL PRIMARY KEY,
    vehicle_id VARCHAR(80) NOT NULL REFERENCES vehicles(id),
    trip_id VARCHAR(80) REFERENCES trips(id),
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    speed_kph NUMERIC(10, 2) NOT NULL,
    fuel_percent NUMERIC(5, 2) NOT NULL,
    odometer_km NUMERIC(12, 2) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    trip_id VARCHAR(80) NOT NULL REFERENCES trips(id),
    vehicle_id VARCHAR(80) NOT NULL REFERENCES vehicles(id),
    type VARCHAR(40) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(600) NOT NULL,
    status VARCHAR(40) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    trip_id VARCHAR(80) NOT NULL REFERENCES trips(id),
    vehicle_id VARCHAR(80) NOT NULL REFERENCES vehicles(id),
    type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(800) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolution_note VARCHAR(800)
);

CREATE TABLE telemetry_events (
    event_id VARCHAR(120) PRIMARY KEY,
    vehicle_id VARCHAR(80) NOT NULL REFERENCES vehicles(id),
    trip_id VARCHAR(80) REFERENCES trips(id),
    received_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_alerts_trip_id ON alerts(trip_id);
CREATE INDEX idx_alerts_trip_status ON alerts(trip_id, status);
CREATE INDEX idx_alerts_trip_severity ON alerts(trip_id, severity);
CREATE INDEX idx_vehicle_positions_vehicle_recorded
    ON vehicle_positions(vehicle_id, recorded_at DESC);
CREATE INDEX idx_incidents_trip_status ON incidents(trip_id, status);
