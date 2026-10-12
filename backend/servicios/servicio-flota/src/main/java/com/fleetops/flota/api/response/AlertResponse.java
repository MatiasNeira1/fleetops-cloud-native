package com.fleetops.flota.api.response;

import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.domain.AlertType;
import java.time.Instant;

public record AlertResponse(
    Long id,
    String tripId,
    String vehicleId,
    AlertType type,
    AlertSeverity severity,
    String title,
    String description,
    AlertStatus status,
    Instant detectedAt
) {
}
