package com.fleetops.flota.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vehicle_positions")
public class VehiclePosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vehicleId;

    private String tripId;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal speedKph;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal fuelPercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal odometerKm;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false)
    private Instant receivedAt;

    protected VehiclePosition() {
    }

    public VehiclePosition(
        String vehicleId,
        String tripId,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal speedKph,
        BigDecimal fuelPercent,
        BigDecimal odometerKm,
        Instant recordedAt,
        Instant receivedAt
    ) {
        this.vehicleId = vehicleId;
        this.tripId = tripId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speedKph = speedKph;
        this.fuelPercent = fuelPercent;
        this.odometerKm = odometerKm;
        this.recordedAt = recordedAt;
        this.receivedAt = receivedAt;
    }

    public Long getId() { return id; }
    public String getVehicleId() { return vehicleId; }
    public String getTripId() { return tripId; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public BigDecimal getSpeedKph() { return speedKph; }
    public BigDecimal getFuelPercent() { return fuelPercent; }
    public BigDecimal getOdometerKm() { return odometerKm; }
    public Instant getRecordedAt() { return recordedAt; }
    public Instant getReceivedAt() { return receivedAt; }
}
