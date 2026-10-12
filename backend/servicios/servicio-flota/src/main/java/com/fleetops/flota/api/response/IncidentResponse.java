package com.fleetops.flota.api.response;

import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.IncidentStatus;
import com.fleetops.flota.domain.IncidentType;
import java.math.BigDecimal;
import java.time.Instant;

public record IncidentResponse(
    Long id,
    String tripId,
    String vehicleId,
    IncidentType type,
    String title,
    String description,
    AlertSeverity severity,
    IncidentStatus status,
    BigDecimal latitude,
    BigDecimal longitude,
    Instant createdAt,
    Instant resolvedAt,
    String resolutionNote
) {
}
