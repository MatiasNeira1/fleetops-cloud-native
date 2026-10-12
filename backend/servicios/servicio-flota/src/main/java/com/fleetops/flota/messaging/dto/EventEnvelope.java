package com.fleetops.flota.messaging.dto;

import java.time.Instant;

public record EventEnvelope<T>(
    String eventId,
    String eventType,
    int eventVersion,
    String aggregateId,
    String correlationId,
    Instant occurredAt,
    T payload
) {
}
