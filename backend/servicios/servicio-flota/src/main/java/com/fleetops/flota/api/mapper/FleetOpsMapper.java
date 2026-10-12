package com.fleetops.flota.api.mapper;

import com.fleetops.flota.api.response.AlertResponse;
import com.fleetops.flota.api.response.DriverResponse;
import com.fleetops.flota.api.response.IncidentResponse;
import com.fleetops.flota.api.response.PlaceResponse;
import com.fleetops.flota.api.response.TimelineItemResponse;
import com.fleetops.flota.api.response.TripDetailResponse;
import com.fleetops.flota.api.response.TripSummaryResponse;
import com.fleetops.flota.api.response.VehiclePositionResponse;
import com.fleetops.flota.api.response.VehicleResponse;
import com.fleetops.flota.domain.Alert;
import com.fleetops.flota.domain.Incident;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.VehiclePosition;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FleetOpsMapper {

    public TripSummaryResponse toSummary(Trip trip) {
        return new TripSummaryResponse(
            trip.getId(),
            origin(trip),
            destination(trip),
            new DriverResponse(trip.getDriverId(), trip.getDriverName()),
            new VehicleResponse(trip.getVehicleId(), trip.getVehiclePlate()),
            trip.getStatus(),
            trip.getDepartureTime(),
            trip.getEstimatedArrival(),
            trip.getProgress(),
            trip.getTotalDistanceKm(),
            trip.getTravelledDistanceKm()
        );
    }

    public TripDetailResponse toDetail(
        Trip trip,
        VehiclePosition lastPosition,
        List<Alert> alerts
    ) {
        return new TripDetailResponse(
            trip.getId(),
            origin(trip),
            destination(trip),
            new DriverResponse(trip.getDriverId(), trip.getDriverName()),
            new VehicleResponse(trip.getVehicleId(), trip.getVehiclePlate()),
            trip.getStatus(),
            trip.getDepartureTime(),
            trip.getEstimatedArrival(),
            trip.getProgress(),
            trip.getTotalDistanceKm(),
            trip.getTravelledDistanceKm(),
            lastPosition == null ? null : toPosition(lastPosition),
            List.of(
                List.of(trip.getOriginLatitude(), trip.getOriginLongitude()),
                List.of(trip.getDestinationLatitude(), trip.getDestinationLongitude())
            ),
            alerts.stream().map(this::toAlert).toList(),
            List.of(
                new TimelineItemResponse("Viaje creado", "completed"),
                new TimelineItemResponse("Vehiculo asignado", "completed"),
                new TimelineItemResponse("En ruta", "current"),
                new TimelineItemResponse("Entregado", "pending")
            )
        );
    }

    public VehiclePositionResponse toPosition(VehiclePosition position) {
        return new VehiclePositionResponse(
            position.getVehicleId(),
            position.getTripId(),
            position.getLatitude(),
            position.getLongitude(),
            position.getSpeedKph(),
            position.getFuelPercent(),
            position.getOdometerKm(),
            position.getRecordedAt()
        );
    }

    public AlertResponse toAlert(Alert alert) {
        return new AlertResponse(
            alert.getId(),
            alert.getTripId(),
            alert.getVehicleId(),
            alert.getType(),
            alert.getSeverity(),
            alert.getTitle(),
            alert.getDescription(),
            alert.getStatus(),
            alert.getDetectedAt()
        );
    }

    public IncidentResponse toIncident(Incident incident) {
        return new IncidentResponse(
            incident.getId(),
            incident.getTripId(),
            incident.getVehicleId(),
            incident.getType(),
            incident.getTitle(),
            incident.getDescription(),
            incident.getSeverity(),
            incident.getStatus(),
            incident.getLatitude(),
            incident.getLongitude(),
            incident.getCreatedAt(),
            incident.getResolvedAt(),
            incident.getResolutionNote()
        );
    }

    private PlaceResponse origin(Trip trip) {
        return new PlaceResponse(
            trip.getOriginName(),
            trip.getOriginLatitude(),
            trip.getOriginLongitude()
        );
    }

    private PlaceResponse destination(Trip trip) {
        return new PlaceResponse(
            trip.getDestinationName(),
            trip.getDestinationLatitude(),
            trip.getDestinationLongitude()
        );
    }
}
