package com.fleetops.flota.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tripId;

    @Column(nullable = false)
    private String vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 800)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    @Column(length = 800)
    private String resolutionNote;

    protected Incident() {
    }

    public Incident(
        String tripId,
        String vehicleId,
        IncidentType type,
        String title,
        String description,
        AlertSeverity severity,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant createdAt
    ) {
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = IncidentStatus.OPEN;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getTripId() { return tripId; }
    public String getVehicleId() { return vehicleId; }
    public IncidentType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AlertSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }

    public void changeStatus(IncidentStatus newStatus, String resolutionNote) {
        boolean allowed = (this.status == IncidentStatus.OPEN
                && (newStatus == IncidentStatus.ACKNOWLEDGED || newStatus == IncidentStatus.RESOLVED))
            || (this.status == IncidentStatus.ACKNOWLEDGED && newStatus == IncidentStatus.RESOLVED);
        if (!allowed) {
            throw new IllegalStateException("Transicion de estado no permitida: " + this.status + " -> " + newStatus);
        }
        if (newStatus == IncidentStatus.RESOLVED) {
            this.resolvedAt = Instant.now();
            this.resolutionNote = resolutionNote;
        }
        this.status = newStatus;
    }
}
