package com.fleetops.flota.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetops.flota.api.error.ConflictException;
import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.request.CreateIncidentRequest;
import com.fleetops.flota.api.request.UpdateIncidentStatusRequest;
import com.fleetops.flota.api.response.IncidentResponse;
import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.Incident;
import com.fleetops.flota.domain.IncidentStatus;
import com.fleetops.flota.domain.IncidentType;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.infrastructure.persistence.IncidentRepository;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import com.fleetops.flota.messaging.producer.DomainEventPublisher;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTests {

    @Mock IncidentRepository incidentRepository;
    @Mock TripRepository tripRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock FleetOpsMapper mapper;
    @Mock DomainEventPublisher publisher;
    @Mock Trip trip;

    private IncidentService service;

    @BeforeEach
    void setUp() {
        service = new IncidentService(incidentRepository, tripRepository, vehicleRepository, mapper, publisher);
    }

    @Test
    void createsIncidentUpdatesTripAndPublishesDomainEvents() {
        IncidentResponse response = org.mockito.Mockito.mock(IncidentResponse.class);
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(trip.getVehicleId()).thenReturn("vehicle-1");
        when(vehicleRepository.existsById("vehicle-1")).thenReturn(true);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(call -> call.getArgument(0));
        when(mapper.toIncident(any(Incident.class))).thenReturn(response);

        assertEquals(response, service.create(createRequest()));

        verify(trip).markIncident();
        verify(publisher).publish(eq("fleetops.incident.created"), any());
        verify(publisher).publish(eq("fleetops.trip.status.changed"), any());
    }

    @Test
    void rejectsIncidentForVehicleDifferentFromTrip() {
        when(tripRepository.findById("trip-1")).thenReturn(Optional.of(trip));
        when(trip.getVehicleId()).thenReturn("vehicle-other");
        when(vehicleRepository.existsById("vehicle-1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(createRequest()));
        verify(incidentRepository, never()).save(any());
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void changesStatusAndPublishesEvent() {
        Incident incident = new Incident("trip-1", "vehicle-1", IncidentType.GENERAL,
            "Incidente", "Detalle", AlertSeverity.WARNING, null, null, Instant.now());
        IncidentResponse response = org.mockito.Mockito.mock(IncidentResponse.class);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(mapper.toIncident(incident)).thenReturn(response);

        assertEquals(response, service.updateStatus(1L,
            new UpdateIncidentStatusRequest(IncidentStatus.ACKNOWLEDGED, null)));

        assertEquals(IncidentStatus.ACKNOWLEDGED, incident.getStatus());
        verify(publisher).publish(eq("fleetops.incident.status.changed"), any());
    }

    @Test
    void returnsNotFoundForUnknownIncident() {
        when(incidentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.updateStatus(404L, new UpdateIncidentStatusRequest(IncidentStatus.RESOLVED, "ok")));
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void rejectsDisallowedStatusTransition() {
        Incident incident = new Incident("trip-1", "vehicle-1", IncidentType.GENERAL,
            "Incidente", "Detalle", AlertSeverity.WARNING, null, null, Instant.now());
        incident.changeStatus(IncidentStatus.RESOLVED, "Cerrado");
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class,
            () -> service.updateStatus(1L, new UpdateIncidentStatusRequest(IncidentStatus.OPEN, null)));
        verify(publisher, never()).publish(any(), any());
    }

    private static CreateIncidentRequest createRequest() {
        return new CreateIncidentRequest("trip-1", "vehicle-1", IncidentType.MECHANICAL_FAILURE,
            "Problema mecanico", "Descripcion", AlertSeverity.CRITICAL, null, null);
    }
}
