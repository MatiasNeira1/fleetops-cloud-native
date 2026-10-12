package com.fleetops.flota.application.service;

import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.request.TelemetryRequest;
import com.fleetops.flota.api.response.TelemetryAcceptedResponse;
import com.fleetops.flota.domain.Alert;
import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.domain.AlertType;
import com.fleetops.flota.domain.TelemetryEvent;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.VehiclePosition;
import com.fleetops.flota.infrastructure.persistence.AlertRepository;
import com.fleetops.flota.infrastructure.persistence.TelemetryEventRepository;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import com.fleetops.flota.messaging.dto.TelemetryEventPayload;
import com.fleetops.flota.messaging.dto.TelemetryReceivedEvent;
import com.fleetops.flota.messaging.dto.EventEnvelope;
import com.fleetops.flota.messaging.producer.DomainEventPublisher;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryService {

    public static final String TELEMETRY_ROUTING_KEY = "fleetops.telemetry.received";
    public static final String ALERT_ROUTING_KEY = "fleetops.alert.created";

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final VehiclePositionRepository positionRepository;
    private final TelemetryEventRepository telemetryEventRepository;
    private final AlertRepository alertRepository;
    private final DomainEventPublisher publisher;
    private final TelemetryProperties properties;

    public TelemetryService(
        VehicleRepository vehicleRepository,
        TripRepository tripRepository,
        VehiclePositionRepository positionRepository,
        TelemetryEventRepository telemetryEventRepository,
        AlertRepository alertRepository,
        DomainEventPublisher publisher,
        TelemetryProperties properties
    ) {
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.positionRepository = positionRepository;
        this.telemetryEventRepository = telemetryEventRepository;
        this.alertRepository = alertRepository;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Transactional
    public TelemetryAcceptedResponse accept(TelemetryRequest request) {
        validateReferences(request);
        if (telemetryEventRepository.existsById(request.eventId())) {
            return new TelemetryAcceptedResponse(request.eventId(), "DUPLICATE_IGNORED", true);
        }

        TelemetryEventPayload payload = toPayload(request);
        TelemetryReceivedEvent event = new TelemetryReceivedEvent(
            request.eventId(),
            "telemetry.received",
            1,
            request.vehicleId(),
            UUID.randomUUID().toString(),
            Instant.now(),
            payload
        );
        publisher.publish(TELEMETRY_ROUTING_KEY, event);
        return new TelemetryAcceptedResponse(request.eventId(), "ACCEPTED", false);
    }

    @Transactional
    public void process(TelemetryEventPayload payload) {
        if (telemetryEventRepository.existsById(payload.eventId())) {
            return;
        }
        validateReferences(payload);
        if (payload.tripId() != null) {
            tripRepository.findById(payload.tripId()).ifPresent(Trip::recordTelemetry);
        }
        positionRepository.save(new VehiclePosition(
            payload.vehicleId(),
            payload.tripId(),
            payload.latitude(),
            payload.longitude(),
            payload.speedKph(),
            payload.fuelPercent(),
            payload.odometerKm(),
            payload.recordedAt(),
            Instant.now()
        ));
        telemetryEventRepository.save(new TelemetryEvent(
            payload.eventId(),
            payload.vehicleId(),
            payload.tripId(),
            Instant.now()
        ));
        createTelemetryAlerts(payload);
    }

    private void createTelemetryAlerts(TelemetryEventPayload payload) {
        if (payload.tripId() == null) {
            return;
        }
        if (payload.speedKph().compareTo(properties.maxSpeedKph()) > 0) {
            publishAlert(alertRepository.save(new Alert(
                payload.tripId(),
                payload.vehicleId(),
                AlertType.SPEEDING,
                AlertSeverity.WARNING,
                "Exceso de velocidad",
                "Velocidad reportada sobre el limite configurado",
                AlertStatus.OPEN,
                Instant.now()
            )));
        }
        if (payload.fuelPercent().compareTo(properties.minFuelPercent()) < 0) {
            publishAlert(alertRepository.save(new Alert(
                payload.tripId(),
                payload.vehicleId(),
                AlertType.LOW_FUEL,
                AlertSeverity.CRITICAL,
                "Combustible bajo",
                "Porcentaje de combustible bajo el minimo configurado",
                AlertStatus.OPEN,
                Instant.now()
            )));
        }
    }

    private void publishAlert(Alert alert) {
        publisher.publish(ALERT_ROUTING_KEY, new EventEnvelope<>(
            UUID.randomUUID().toString(),
            "alert.created",
            1,
            String.valueOf(alert.getId()),
            UUID.randomUUID().toString(),
            Instant.now(),
            alert
        ));
    }

    private void validateReferences(TelemetryRequest request) {
        if (!vehicleRepository.existsById(request.vehicleId())) {
            throw new ResourceNotFoundException("No existe el vehiculo " + request.vehicleId());
        }
        if (request.tripId() != null) {
            var trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el viaje " + request.tripId()));
            if (!trip.getVehicleId().equals(request.vehicleId())) {
                throw new IllegalArgumentException("El vehiculo no corresponde al viaje informado");
            }
        }
    }

    private void validateReferences(TelemetryEventPayload payload) {
        if (!vehicleRepository.existsById(payload.vehicleId())) {
            throw new ResourceNotFoundException("No existe el vehiculo " + payload.vehicleId());
        }
        if (payload.tripId() != null) {
            var trip = tripRepository.findById(payload.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("No existe el viaje " + payload.tripId()));
            if (!trip.getVehicleId().equals(payload.vehicleId())) {
                throw new IllegalArgumentException("El vehiculo no corresponde al viaje informado");
            }
        }
    }

    private TelemetryEventPayload toPayload(TelemetryRequest request) {
        return new TelemetryEventPayload(
            request.eventId(),
            request.vehicleId(),
            request.tripId(),
            request.latitude(),
            request.longitude(),
            request.speedKph(),
            request.fuelPercent(),
            request.odometerKm(),
            request.recordedAt()
        );
    }
}
