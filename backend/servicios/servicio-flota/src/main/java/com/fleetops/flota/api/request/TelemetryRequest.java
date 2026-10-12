package com.fleetops.flota.api.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record TelemetryRequest(
    @NotBlank String eventId,
    @NotBlank String vehicleId,
    String tripId,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    @NotNull @DecimalMin("0.0") BigDecimal speedKph,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal fuelPercent,
    @NotNull @DecimalMin("0.0") BigDecimal odometerKm,
    @NotNull Instant recordedAt
) {
}
