package com.fleetops.flota.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "telemetry_events")
public class TelemetryEvent {

    @Id
    private String eventId;

    @Column(nullable = false)
    private String vehicleId;

    private String tripId;

    @Column(nullable = false)
    private Instant receivedAt;

    private String source;

    protected TelemetryEvent() {
    }

    public TelemetryEvent(String eventId, String vehicleId, String tripId, Instant receivedAt) {
        this.eventId = eventId;
        this.vehicleId = vehicleId;
        this.tripId = tripId;
        this.receivedAt = receivedAt;
    }

    public TelemetryEvent(String eventId, String vehicleId, String tripId, Instant receivedAt,
                          String source) {
        this(eventId, vehicleId, tripId, receivedAt);
        this.source = source;
    }

    public String getEventId() { return eventId; }
    public String getVehicleId() { return vehicleId; }
    public String getTripId() { return tripId; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getSource() { return source; }
}
