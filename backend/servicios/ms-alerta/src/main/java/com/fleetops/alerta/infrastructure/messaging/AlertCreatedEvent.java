package com.fleetops.alerta.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.fleetops.alerta.domain.model.AlertType;
import com.fleetops.alerta.domain.model.AlertSeverity;

public record AlertCreatedEvent(
    UUID eventId, String eventType, int eventVersion, Instant occurredAt,
    String source, String correlationId, Long alertId, String tripId,
    String vehicleId, String alertType, String severity, String title,
    String message, BigDecimal measuredValue, BigDecimal thresholdValue,
    Map<String, String> metadata
) {
    public void validate() {
        if (eventId == null || !"alert.created".equals(eventType) || eventVersion != 1
            || occurredAt == null || blank(source) || blank(correlationId)
            || alertId == null || blank(tripId) || blank(vehicleId)
            || blank(alertType) || blank(severity)
            || blank(title) || blank(message) || metadata == null
            || measuredValue == null || thresholdValue == null
            || measuredValue.signum() < 0 || thresholdValue.signum() < 0) {
            throw new IllegalArgumentException("Evento de alerta inválido");
        }
        AlertType.valueOf(alertType);
        AlertSeverity.valueOf(severity);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
