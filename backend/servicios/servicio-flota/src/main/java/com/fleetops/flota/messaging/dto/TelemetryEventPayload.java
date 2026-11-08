package com.fleetops.flota.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TelemetryEventPayload(
    String eventId,
    String vehicleId,
    String tripId,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal speedKph,
    BigDecimal fuelPercent,
    BigDecimal odometerKm,
    Instant recordedAt,
    String source
) {
    public TelemetryEventPayload(String eventId, String vehicleId, String tripId,
                                 BigDecimal latitude, BigDecimal longitude, BigDecimal speedKph,
                                 BigDecimal fuelPercent, BigDecimal odometerKm, Instant recordedAt) {
        this(eventId, vehicleId, tripId, latitude, longitude, speedKph, fuelPercent,
            odometerKm, recordedAt, null);
    }
}
