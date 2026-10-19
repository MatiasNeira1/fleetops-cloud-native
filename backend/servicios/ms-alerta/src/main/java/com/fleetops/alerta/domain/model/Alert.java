package com.fleetops.alerta.domain.model;

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
import java.util.UUID;

@Entity
@Table(name = "alerts", schema = "alerts")
public class Alert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private UUID sourceEventId;
    private Long sourceAlertId;
    @Column(nullable = false, length = 120) private String correlationId;
    @Column(nullable = false) private String tripId;
    @Column(nullable = false) private String vehicleId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AlertType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AlertSeverity severity;
    @Column(nullable = false) private String title;
    @Column(nullable = false, length = 600) private String message;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal measuredValue;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal thresholdValue;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AlertStatus status;
    @Column(nullable = false) private Instant occurredAt;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected Alert() {}

    public Alert(UUID sourceEventId, Long sourceAlertId, String correlationId, String tripId, String vehicleId,
                 AlertType type, AlertSeverity severity, String title, String message,
                 BigDecimal measuredValue, BigDecimal thresholdValue, Instant occurredAt) {
        this.sourceEventId = sourceEventId;
        this.sourceAlertId = sourceAlertId;
        this.correlationId = correlationId;
        this.tripId = tripId;
        this.vehicleId = vehicleId;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.measuredValue = measuredValue;
        this.thresholdValue = thresholdValue;
        this.status = AlertStatus.ACTIVE;
        this.occurredAt = occurredAt;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() { return id; }
    public UUID getSourceEventId() { return sourceEventId; }
    public Long getSourceAlertId() { return sourceAlertId; }
    public String getCorrelationId() { return correlationId; }
    public String getTripId() { return tripId; }
    public String getVehicleId() { return vehicleId; }
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public BigDecimal getMeasuredValue() { return measuredValue; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public AlertStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void changeStatus(AlertStatus next) {
        if (next == null || next == AlertStatus.ACTIVE || status == AlertStatus.RESOLVED
            || status == next || (status == AlertStatus.ACKNOWLEDGED && next != AlertStatus.RESOLVED)) {
            throw new IllegalArgumentException("Transición de estado no permitida");
        }
        status = next;
        updatedAt = Instant.now();
    }
}
