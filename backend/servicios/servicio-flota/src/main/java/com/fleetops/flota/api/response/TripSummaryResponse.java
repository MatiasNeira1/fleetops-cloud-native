package com.fleetops.flota.api.response;

import com.fleetops.flota.domain.TripStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TripSummaryResponse(
    String id,
    PlaceResponse origin,
    PlaceResponse destination,
    DriverResponse driver,
    VehicleResponse vehicle,
    TripStatus status,
    Instant departureTime,
    Instant estimatedArrival,
    int progress,
    BigDecimal totalDistanceKm,
    BigDecimal travelledDistanceKm
) {
}
