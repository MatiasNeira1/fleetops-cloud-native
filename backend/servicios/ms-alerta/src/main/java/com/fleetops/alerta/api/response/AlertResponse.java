package com.fleetops.alerta.api.response;

import com.fleetops.alerta.domain.model.Alert;
import com.fleetops.alerta.domain.model.AlertSeverity;
import com.fleetops.alerta.domain.model.AlertStatus;
import com.fleetops.alerta.domain.model.AlertType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
    Long id, UUID sourceEventId, String correlationId, String tripId, String vehicleId,
    AlertType type, AlertSeverity severity, String title, String message,
    BigDecimal measuredValue, BigDecimal thresholdValue, AlertStatus status,
    Instant occurredAt, Instant createdAt, Instant updatedAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getSourceEventId(), alert.getCorrelationId(), alert.getTripId(),
            alert.getVehicleId(), alert.getType(), alert.getSeverity(), alert.getTitle(),
            alert.getMessage(), alert.getMeasuredValue(), alert.getThresholdValue(),
            alert.getStatus(), alert.getOccurredAt(), alert.getCreatedAt(), alert.getUpdatedAt());
    }
}
