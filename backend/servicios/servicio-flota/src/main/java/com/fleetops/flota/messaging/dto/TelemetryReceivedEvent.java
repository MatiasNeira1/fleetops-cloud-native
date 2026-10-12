package com.fleetops.flota.messaging.dto;

import java.time.Instant;

public record TelemetryReceivedEvent(
    String eventId,
    String eventType,
    int eventVersion,
    String aggregateId,
    String correlationId,
    Instant occurredAt,
    TelemetryEventPayload payload
) {
}
