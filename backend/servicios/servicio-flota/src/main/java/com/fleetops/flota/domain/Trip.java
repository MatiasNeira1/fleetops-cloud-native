package com.fleetops.flota.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    private String id;

    @Column(nullable = false)
    private String originName;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(nullable = false)
    private String destinationName;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLatitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal destinationLongitude;

    @Column(nullable = false)
    private Instant departureTime;

    @Column(nullable = false)
    private Instant estimatedArrival;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TripStatus status;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDistanceKm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal travelledDistanceKm;

    @Column(nullable = false)
    private String vehicleId;

    @Column(nullable = false)
    private String vehiclePlate;

    @Column(nullable = false)
    private String driverId;

    @Column(nullable = false)
    private String driverName;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Trip() {
    }

    public Trip(
        String id,
        String originName,
        BigDecimal originLatitude,
        BigDecimal originLongitude,
        String destinationName,
        BigDecimal destinationLatitude,
        BigDecimal destinationLongitude,
        Instant departureTime,
        Instant estimatedArrival,
        TripStatus status,
        int progress,
        BigDecimal totalDistanceKm,
        BigDecimal travelledDistanceKm,
        String vehicleId,
        String vehiclePlate,
        String driverId,
        String driverName,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.originName = originName;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationName = destinationName;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.departureTime = departureTime;
        this.estimatedArrival = estimatedArrival;
        this.status = status;
        this.progress = progress;
        this.totalDistanceKm = totalDistanceKm;
        this.travelledDistanceKm = travelledDistanceKm;
        this.vehicleId = vehicleId;
        this.vehiclePlate = vehiclePlate;
        this.driverId = driverId;
        this.driverName = driverName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getOriginName() { return originName; }
    public BigDecimal getOriginLatitude() { return originLatitude; }
    public BigDecimal getOriginLongitude() { return originLongitude; }
    public String getDestinationName() { return destinationName; }
    public BigDecimal getDestinationLatitude() { return destinationLatitude; }
    public BigDecimal getDestinationLongitude() { return destinationLongitude; }
    public Instant getDepartureTime() { return departureTime; }
    public Instant getEstimatedArrival() { return estimatedArrival; }
    public TripStatus getStatus() { return status; }
    public int getProgress() { return progress; }
    public BigDecimal getTotalDistanceKm() { return totalDistanceKm; }
    public BigDecimal getTravelledDistanceKm() { return travelledDistanceKm; }
    public String getVehicleId() { return vehicleId; }
    public String getVehiclePlate() { return vehiclePlate; }
    public String getDriverId() { return driverId; }
    public String getDriverName() { return driverName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateProgress(int progress, BigDecimal travelledDistanceKm) {
        this.progress = progress;
        this.travelledDistanceKm = travelledDistanceKm;
        this.updatedAt = Instant.now();
    }

    public void markIncident() {
        this.status = TripStatus.INCIDENT;
        this.updatedAt = Instant.now();
    }

    public void recordTelemetry() {
        if (this.status == TripStatus.SCHEDULED) {
            this.status = TripStatus.IN_TRANSIT;
        }
        this.updatedAt = Instant.now();
    }
}
