package com.fleetops.flota.application.service;

import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.response.AlertResponse;
import com.fleetops.flota.api.response.TripDetailResponse;
import com.fleetops.flota.api.response.TripSummaryResponse;
import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.infrastructure.persistence.AlertRepository;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripQueryService {

    private final TripRepository tripRepository;
    private final AlertRepository alertRepository;
    private final VehiclePositionRepository positionRepository;
    private final FleetOpsMapper mapper;

    public TripQueryService(
        TripRepository tripRepository,
        AlertRepository alertRepository,
        VehiclePositionRepository positionRepository,
        FleetOpsMapper mapper
    ) {
        this.tripRepository = tripRepository;
        this.alertRepository = alertRepository;
        this.positionRepository = positionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TripSummaryResponse> activeTrips(TripStatus status) {
        List<Trip> trips = status == null
            ? tripRepository.findByStatusNotIn(
                EnumSet.of(TripStatus.COMPLETED, TripStatus.CANCELLED)
            )
            : tripRepository.findByStatus(status);

        return trips.stream()
            .filter(trip -> trip.getStatus() != TripStatus.COMPLETED)
            .filter(trip -> trip.getStatus() != TripStatus.CANCELLED)
            .map(mapper::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public TripDetailResponse tripDetail(String id) {
        Trip trip = findTrip(id);
        var lastPosition = positionRepository
            .findFirstByVehicleIdOrderByRecordedAtDesc(trip.getVehicleId())
            .orElse(null);
        var alerts = alertRepository.findByTripIdOrderByDetectedAtDesc(id);
        return mapper.toDetail(trip, lastPosition, alerts);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> alerts(String tripId, AlertStatus status, AlertSeverity severity) {
        findTrip(tripId);
        if (status != null) {
            return alertRepository.findByTripIdAndStatusOrderByDetectedAtDesc(tripId, status)
                .stream()
                .map(mapper::toAlert)
                .toList();
        }
        if (severity != null) {
            return alertRepository.findByTripIdAndSeverityOrderByDetectedAtDesc(tripId, severity)
                .stream()
                .map(mapper::toAlert)
                .toList();
        }
        return alertRepository.findByTripIdOrderByDetectedAtDesc(tripId)
            .stream()
            .map(mapper::toAlert)
            .toList();
    }

    private Trip findTrip(String id) {
        return tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No existe el viaje " + id));
    }
}
