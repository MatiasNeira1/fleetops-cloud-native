package com.fleetops.flota.application.service;

import com.fleetops.flota.api.error.ConflictException;
import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.request.CreateIncidentRequest;
import com.fleetops.flota.api.request.UpdateIncidentStatusRequest;
import com.fleetops.flota.api.response.IncidentResponse;
import com.fleetops.flota.domain.Incident;
import com.fleetops.flota.infrastructure.persistence.IncidentRepository;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import com.fleetops.flota.messaging.dto.EventEnvelope;
import com.fleetops.flota.messaging.producer.DomainEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final FleetOpsMapper mapper;
    private final DomainEventPublisher publisher;

    public IncidentService(
        IncidentRepository incidentRepository,
        TripRepository tripRepository,
        VehicleRepository vehicleRepository,
        FleetOpsMapper mapper,
        DomainEventPublisher publisher
    ) {
        this.incidentRepository = incidentRepository;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.mapper = mapper;
        this.publisher = publisher;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        var trip = tripRepository.findById(request.tripId())
            .orElseThrow(() -> new ResourceNotFoundException("No existe el viaje " + request.tripId()));
        if (!vehicleRepository.existsById(request.vehicleId())) {
            throw new ResourceNotFoundException("No existe el vehiculo " + request.vehicleId());
        }
        if (!trip.getVehicleId().equals(request.vehicleId())) {
            throw new ConflictException("El vehiculo no corresponde al viaje informado");
        }
        Incident incident = incidentRepository.save(new Incident(
            request.tripId(),
            request.vehicleId(),
            request.type(),
            request.title(),
            request.description(),
            request.severity(),
            request.latitude(),
            request.longitude(),
            Instant.now()
        ));
        trip.markIncident();
        publisher.publish("fleetops.incident.created", envelope(
            "incident.created",
            String.valueOf(incident.getId()),
            mapper.toIncident(incident)
        ));
        publisher.publish("fleetops.trip.status.changed", envelope(
            "trip.status.changed",
            trip.getId(),
            trip.getStatus()
        ));
        return mapper.toIncident(incident);
    }

    @Transactional
    public IncidentResponse updateStatus(Long id, UpdateIncidentStatusRequest request) {
        Incident incident = incidentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el incidente " + id));
        try {
            incident.changeStatus(request.status(), request.resolutionNote());
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
        publisher.publish("fleetops.incident.status.changed", envelope(
            "incident.status.changed",
            String.valueOf(incident.getId()),
            mapper.toIncident(incident)
        ));
        return mapper.toIncident(incident);
    }

    private EventEnvelope<?> envelope(String type, String aggregateId, Object payload) {
        return new EventEnvelope<>(
            UUID.randomUUID().toString(),
            type,
            1,
            aggregateId,
            UUID.randomUUID().toString(),
            Instant.now(),
            payload
        );
    }
}
