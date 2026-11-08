package com.fleetops.flota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.application.service.DemoTripService;
import com.fleetops.flota.application.service.TelemetryProperties;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.domain.VehiclePosition;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DemoTripServiceTests {
    @Test
    void createsIdentifiedNearThresholdTripAndReusesActiveDemo() {
        TripRepository trips = mock(TripRepository.class);
        VehicleRepository vehicles = mock(VehicleRepository.class);
        VehiclePositionRepository positions = mock(VehiclePositionRepository.class);
        when(trips.findByStatusNotIn(any())).thenReturn(List.of());
        when(trips.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new DemoTripService(trips, vehicles, positions,
            new FleetOpsMapper(new TelemetryProperties(new BigDecimal("90"), new BigDecimal("15"))));

        var result = service.create();
        assertTrue(result.id().startsWith("demo-trip-"));
        assertEquals(TripStatus.IN_TRANSIT, result.status());
        ArgumentCaptor<Trip> trip = ArgumentCaptor.forClass(Trip.class);
        ArgumentCaptor<VehiclePosition> position = ArgumentCaptor.forClass(VehiclePosition.class);
        verify(trips).save(trip.capture());
        verify(positions).save(position.capture());
        assertEquals(trip.getValue().getVehicleId(), position.getValue().getVehicleId());
        assertEquals(new BigDecimal("15.22"), position.getValue().getFuelPercent());
        assertEquals("fleetops-simulator", position.getValue().getSource());

        when(trips.findByStatusNotIn(any())).thenReturn(List.of(trip.getValue()));
        assertEquals(result.id(), service.create().id());
    }
}
