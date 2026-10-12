package com.fleetops.flota.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.response.TripSummaryResponse;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.infrastructure.persistence.AlertRepository;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FleetOpsQueryServiceTests {

    @Mock TripRepository tripRepository;
    @Mock AlertRepository alertRepository;
    @Mock VehiclePositionRepository positionRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock FleetOpsMapper mapper;
    @InjectMocks TripQueryService tripService;
    @InjectMocks VehiclePositionService positionService;

    @Test
    void listsActiveTripsAndAppliesStateFilter() {
        Trip trip = org.mockito.Mockito.mock(Trip.class);
        TripSummaryResponse summary = org.mockito.Mockito.mock(TripSummaryResponse.class);
        when(trip.getStatus()).thenReturn(TripStatus.IN_TRANSIT);
        when(tripRepository.findByStatus(TripStatus.IN_TRANSIT)).thenReturn(List.of(trip));
        when(mapper.toSummary(trip)).thenReturn(summary);

        assertEquals(List.of(summary), tripService.activeTrips(TripStatus.IN_TRANSIT));
        verify(tripRepository).findByStatus(TripStatus.IN_TRANSIT);
    }

    @Test
    void rejectsMissingTripWhenQueryingAlerts() {
        when(tripRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> tripService.alerts("missing", null, null));
    }

    @Test
    void returnsEmptyAlertListForExistingTripWithoutAlerts() {
        Trip trip = org.mockito.Mockito.mock(Trip.class);
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(alertRepository.findByTripIdOrderByDetectedAtDesc("trip-1")).thenReturn(List.of());

        assertEquals(List.of(), tripService.alerts("trip-1", null, null));
    }

    @Test
    void reportsUnknownVehicle() {
        positionService = new VehiclePositionService(vehicleRepository, positionRepository, mapper);
        when(vehicleRepository.existsById("missing")).thenReturn(false);

        var error = assertThrows(ResourceNotFoundException.class,
            () -> positionService.latestPosition("missing"));

        assertEquals("No existe el vehiculo missing", error.getMessage());
    }

    @Test
    void reportsVehicleWithoutPosition() {
        positionService = new VehiclePositionService(vehicleRepository, positionRepository, mapper);
        when(vehicleRepository.existsById("vehicle-1")).thenReturn(true);
        when(positionRepository.findFirstByVehicleIdOrderByRecordedAtDesc("vehicle-1"))
            .thenReturn(Optional.empty());

        var error = assertThrows(ResourceNotFoundException.class,
            () -> positionService.latestPosition("vehicle-1"));

        assertEquals("El vehiculo vehicle-1 todavia no tiene posicion registrada", error.getMessage());
    }
}
