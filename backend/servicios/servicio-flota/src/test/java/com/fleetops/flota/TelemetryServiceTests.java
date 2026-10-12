package com.fleetops.flota.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.request.TelemetryRequest;
import com.fleetops.flota.domain.Alert;
import com.fleetops.flota.domain.TelemetryEvent;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.infrastructure.persistence.AlertRepository;
import com.fleetops.flota.infrastructure.persistence.TelemetryEventRepository;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import com.fleetops.flota.messaging.producer.DomainEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTests {

    @Mock VehicleRepository vehicleRepository;
    @Mock TripRepository tripRepository;
    @Mock VehiclePositionRepository positionRepository;
    @Mock TelemetryEventRepository telemetryEventRepository;
    @Mock AlertRepository alertRepository;
    @Mock DomainEventPublisher publisher;

    private TelemetryService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryService(vehicleRepository, tripRepository, positionRepository,
            telemetryEventRepository, alertRepository, publisher,
            new TelemetryProperties(new BigDecimal("90"), new BigDecimal("15")));
    }

    @Test
    void acceptsTelemetryByPublishingEvent() {
        when(vehicleRepository.existsById("vehicle-1")).thenReturn(true);
        Trip trip = trip("vehicle-1");
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(telemetryEventRepository.existsById("event-1")).thenReturn(false);

        var response = service.accept(request("event-1", "vehicle-1", "trip-1"));

        assertEquals("ACCEPTED", response.status());
        assertEquals(false, response.duplicate());
        verify(publisher).publish(eq(TelemetryService.TELEMETRY_ROUTING_KEY), any());
    }

    @Test
    void duplicateEventIsNotPublishedAgain() {
        when(vehicleRepository.existsById("vehicle-1")).thenReturn(true);
        when(telemetryEventRepository.existsById("event-1")).thenReturn(true);

        var response = service.accept(request("event-1", "vehicle-1", null));

        assertEquals("DUPLICATE_IGNORED", response.status());
        assertEquals(true, response.duplicate());
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void rejectsUnknownVehicle() {
        when(vehicleRepository.existsById("missing")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> service.accept(request("event-1", "missing", null)));
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void processingPersistsPositionAndPublishesSpeedAndFuelAlerts() {
        when(telemetryEventRepository.existsById("event-1")).thenReturn(false);
        when(vehicleRepository.existsById("vehicle-1")).thenReturn(true);
        Trip trip = trip("vehicle-1");
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.process(new com.fleetops.flota.messaging.dto.TelemetryEventPayload(
            "event-1", "vehicle-1", "trip-1", new BigDecimal("-33.4"),
            new BigDecimal("-70.6"), new BigDecimal("105"), new BigDecimal("8"),
            new BigDecimal("1234"), Instant.parse("2026-10-11T12:00:00Z")));

        verify(positionRepository).save(any());
        verify(telemetryEventRepository).save(any(TelemetryEvent.class));
        verify(alertRepository, org.mockito.Mockito.times(2)).save(any(Alert.class));
        verify(publisher, org.mockito.Mockito.times(2)).publish(eq(TelemetryService.ALERT_ROUTING_KEY), any());
    }

    private static TelemetryRequest request(String eventId, String vehicleId, String tripId) {
        return new TelemetryRequest(eventId, vehicleId, tripId,
            new BigDecimal("-33.4"), new BigDecimal("-70.6"), new BigDecimal("50"),
            new BigDecimal("50"), new BigDecimal("1234"), Instant.parse("2026-10-11T12:00:00Z"));
    }

    private static Trip trip(String vehicleId) {
        Trip trip = org.mockito.Mockito.mock(Trip.class);
        when(trip.getVehicleId()).thenReturn(vehicleId);
        return trip;
    }
}
