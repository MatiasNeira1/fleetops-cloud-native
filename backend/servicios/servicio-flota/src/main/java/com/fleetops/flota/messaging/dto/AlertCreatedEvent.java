package com.fleetops.flota.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AlertCreatedEvent(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String source,
    String correlationId,
    Long alertId,
    String tripId,
    String vehicleId,
    String alertType,
    String severity,
    String title,
    String message,
    BigDecimal measuredValue,
    BigDecimal thresholdValue,
    Map<String, String> metadata
) {
}
