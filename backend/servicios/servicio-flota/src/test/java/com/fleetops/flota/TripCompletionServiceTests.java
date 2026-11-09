package com.fleetops.flota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fleetops.flota.api.error.ConflictException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.application.service.TelemetryProperties;
import com.fleetops.flota.application.service.TripCompletionService;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.domain.VehiclePosition;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TripCompletionServiceTests {
    @Test
    void requiresArrivalFromSameSimulationAndCompletesIdempotently() {
        TripRepository trips = mock(TripRepository.class);
        VehiclePositionRepository positions = mock(VehiclePositionRepository.class);
        Trip trip = trip();
        when(trips.findById("demo-trip-test")).thenReturn(Optional.of(trip));
        var service = new TripCompletionService(trips, positions,
            new FleetOpsMapper(new TelemetryProperties(new BigDecimal("90"), new BigDecimal("15"))));
        when(positions.findFirstByVehicleIdOrderByRecordedAtDesc("demo-vehicle-test"))
            .thenReturn(Optional.of(position("other-trip", BigDecimal.ZERO)));
        assertThrows(ConflictException.class, () -> service.complete(trip.getId()));
        when(positions.findFirstByVehicleIdOrderByRecordedAtDesc("demo-vehicle-test"))
            .thenReturn(Optional.of(position(trip.getId(), BigDecimal.ONE)));
        assertThrows(ConflictException.class, () -> service.complete(trip.getId()));
        when(positions.findFirstByVehicleIdOrderByRecordedAtDesc("demo-vehicle-test"))
            .thenReturn(Optional.of(position(trip.getId(), BigDecimal.ZERO)));
        assertEquals(TripStatus.COMPLETED, service.complete(trip.getId()).status());
        assertEquals(TripStatus.COMPLETED, service.complete(trip.getId()).status());
    }

    private Trip trip() {
        Instant now = Instant.now();
        return new Trip("demo-trip-test", "A", BigDecimal.ZERO, BigDecimal.ZERO,
            "B", BigDecimal.ZERO, BigDecimal.ONE, now, now.plusSeconds(300),
            TripStatus.IN_TRANSIT, 90, BigDecimal.ONE, new BigDecimal("0.9"),
            "demo-vehicle-test", "SIM", "driver", "Driver", now, now);
    }

    private VehiclePosition position(String tripId, BigDecimal speed) {
        Instant now = Instant.now();
        return new VehiclePosition("demo-vehicle-test", tripId, BigDecimal.ZERO,
            BigDecimal.ONE, speed, new BigDecimal("14"), BigDecimal.ONE,
            now, now, "fleetops-simulator");
    }
}
