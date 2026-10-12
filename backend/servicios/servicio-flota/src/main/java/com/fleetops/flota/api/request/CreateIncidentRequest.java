package com.fleetops.flota.api.request;

import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.IncidentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateIncidentRequest(
    @NotBlank String tripId,
    @NotBlank String vehicleId,
    @NotNull IncidentType type,
    @NotBlank String title,
    @NotBlank String description,
    @NotNull AlertSeverity severity,
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude
) {
}
