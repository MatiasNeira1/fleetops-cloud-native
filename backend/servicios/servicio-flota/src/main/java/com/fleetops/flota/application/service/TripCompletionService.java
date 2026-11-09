package com.fleetops.flota.application.service;

import com.fleetops.flota.api.error.ConflictException;
import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.response.TripSummaryResponse;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripCompletionService {
    private final TripRepository trips;
    private final VehiclePositionRepository positions;
    private final FleetOpsMapper mapper;

    public TripCompletionService(TripRepository trips, VehiclePositionRepository positions,
                                 FleetOpsMapper mapper) {
        this.trips = trips;
        this.positions = positions;
        this.mapper = mapper;
    }

    @Transactional
    public TripSummaryResponse complete(String tripId) {
        var trip = trips.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el viaje " + tripId));
        if (trip.getStatus() == TripStatus.COMPLETED) {
            return mapper.toSummary(trip);
        }
        if (trip.getStatus() == TripStatus.CANCELLED || trip.getStatus() == TripStatus.SCHEDULED) {
            throw new ConflictException("El viaje no puede completarse desde el simulador");
        }
        var position = positions.findFirstByVehicleIdOrderByRecordedAtDesc(trip.getVehicleId())
            .orElseThrow(() -> new ConflictException("No hay telemetria final del viaje"));
        if (!tripId.equals(position.getTripId()) || !"fleetops-simulator".equals(position.getSource())) {
            throw new ConflictException("La ultima posicion no pertenece a esta simulacion");
        }
        double lat = position.getLatitude().doubleValue() - trip.getDestinationLatitude().doubleValue();
        double lon = position.getLongitude().doubleValue() - trip.getDestinationLongitude().doubleValue();
        if (Math.hypot(lat, lon) > 0.0005 || position.getSpeedKph().signum() != 0) {
            throw new ConflictException("El vehiculo aun no llego al destino");
        }
        trip.complete();
        return mapper.toSummary(trip);
    }
}
