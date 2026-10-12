package com.fleetops.flota.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tripId;

    @Column(nullable = false)
    private String vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 600)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false)
    private Instant detectedAt;

    protected Alert() {
    }

    public Alert(
        String tripId,
        String vehicleId,
        AlertType type,
        AlertSeverity severity,
        String title,
        String description,
        AlertStatus status,
        Instant detectedAt
    ) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.status = status;
        this.detectedAt = detectedAt;
    }

    public Long getId() { return id; }
    public String getTripId() { return tripId; }
    public String getVehicleId() { return vehicleId; }
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AlertStatus getStatus() { return status; }
    public Instant getDetectedAt() { return detectedAt; }

    public void resolve() {
        this.status = AlertStatus.RESOLVED;
    }
}
