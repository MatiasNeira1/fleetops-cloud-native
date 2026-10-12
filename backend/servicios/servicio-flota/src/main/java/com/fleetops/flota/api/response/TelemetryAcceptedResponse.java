package com.fleetops.flota.api.response;

public record TelemetryAcceptedResponse(
    String eventId,
    String status,
    boolean duplicate
) {
}
