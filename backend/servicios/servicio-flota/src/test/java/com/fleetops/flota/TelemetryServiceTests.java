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
import com.fleetops.flota.messaging.dto.AlertCreatedEvent;
import com.fleetops.flota.messaging.dto.TelemetryReceivedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
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
        ArgumentCaptor<TelemetryReceivedEvent> sent = ArgumentCaptor.forClass(TelemetryReceivedEvent.class);
        verify(publisher).publish(eq(TelemetryService.TELEMETRY_ROUTING_KEY), sent.capture());
        assertEquals("event-1", sent.getValue().correlationId());
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
        verify(publisher).publishAlertAfterCommit(eq(TelemetryService.SPEED_ALERT_ROUTING_KEY), any());
        verify(publisher).publishAlertAfterCommit(eq(TelemetryService.FUEL_ALERT_ROUTING_KEY), any());
    }

    @Test
    void publishesVersionedSpeedAndFuelEventsWithCorrelation() {
        prepareProcessing();
        service.process(payload("105", "8"), "correlation-123");

        ArgumentCaptor<AlertCreatedEvent> speed = ArgumentCaptor.forClass(AlertCreatedEvent.class);
        ArgumentCaptor<AlertCreatedEvent> fuel = ArgumentCaptor.forClass(AlertCreatedEvent.class);
        verify(publisher).publishAlertAfterCommit(eq(TelemetryService.SPEED_ALERT_ROUTING_KEY), speed.capture());
        verify(publisher).publishAlertAfterCommit(eq(TelemetryService.FUEL_ALERT_ROUTING_KEY), fuel.capture());
        assertEquals("SPEED_LIMIT_EXCEEDED", speed.getValue().alertType());
        assertEquals(new BigDecimal("105"), speed.getValue().measuredValue());
        assertEquals(new BigDecimal("90"), speed.getValue().thresholdValue());
        assertEquals("correlation-123", speed.getValue().correlationId());
        assertEquals("LOW_FUEL", fuel.getValue().alertType());
        assertEquals(new BigDecimal("15"), fuel.getValue().thresholdValue());
        assertEquals(1, fuel.getValue().eventVersion());
    }

    @Test
    void normalTelemetryDoesNotCreateOrPublishAlerts() {
        prepareProcessing();
        service.process(payload("80", "50"), "correlation-123");
        verify(alertRepository, never()).save(any(Alert.class));
        verify(publisher, never()).publishAlertAfterCommit(any(), any());
    }

    private void prepareProcessing() {
        when(telemetryEventRepository.existsById("event-1")).thenReturn(false);
        when(vehicleRepository.existsById("vehicle-1")).thenReturn(true);
        Trip trip = trip("vehicle-1");
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        org.mockito.Mockito.lenient().when(alertRepository.save(any(Alert.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static com.fleetops.flota.messaging.dto.TelemetryEventPayload payload(String speed, String fuel) {
        return new com.fleetops.flota.messaging.dto.TelemetryEventPayload(
            "event-1", "vehicle-1", "trip-1", new BigDecimal("-33.4"),
            new BigDecimal("-70.6"), new BigDecimal(speed), new BigDecimal(fuel),
            new BigDecimal("1234"), Instant.parse("2026-10-11T12:00:00Z"));
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
