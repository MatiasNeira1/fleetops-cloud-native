package com.fleetops.flota.api.response;

import java.math.BigDecimal;
import java.time.Instant;

public record VehiclePositionResponse(
    String vehicleId,
    String tripId,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal speedKph,
    BigDecimal fuelPercent,
    BigDecimal odometerKm,
    Instant recordedAt
) {
}
