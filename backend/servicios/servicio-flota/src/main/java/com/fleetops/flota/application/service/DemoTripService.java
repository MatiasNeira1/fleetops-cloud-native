package com.fleetops.flota.application.service;

import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.response.TripSummaryResponse;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.domain.Vehicle;
import com.fleetops.flota.domain.VehiclePosition;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoTripService {
    private final TripRepository trips;
    private final VehicleRepository vehicles;
    private final VehiclePositionRepository positions;
    private final FleetOpsMapper mapper;

    public DemoTripService(TripRepository trips, VehicleRepository vehicles,
                           VehiclePositionRepository positions, FleetOpsMapper mapper) {
        this.trips = trips;
        this.vehicles = vehicles;
        this.positions = positions;
        this.mapper = mapper;
    }

    @Transactional
    public TripSummaryResponse create() {
        var active = trips.findByStatusNotIn(EnumSet.of(TripStatus.COMPLETED, TripStatus.CANCELLED))
            .stream().filter(trip -> trip.getId().startsWith("demo-trip-")).findFirst();
        if (active.isPresent()) return mapper.toSummary(active.get());

        String suffix = UUID.randomUUID().toString();
        String vehicleId = "demo-vehicle-" + suffix;
        String tripId = "demo-trip-" + suffix;
        Instant now = Instant.now();
        vehicles.save(new Vehicle(vehicleId, "SIM-DEMO"));
        Trip trip = trips.save(new Trip(tripId, "Santiago Centro (simulado)",
            new BigDecimal("-33.4489000"), new BigDecimal("-70.6693000"),
            "Parque Bustamante (simulado)", new BigDecimal("-33.4489000"),
            new BigDecimal("-70.6500000"), now, now.plusSeconds(300),
            TripStatus.IN_TRANSIT, 0, new BigDecimal("1.80"), BigDecimal.ZERO,
            vehicleId, "SIM-DEMO", "demo-driver", "Conductor simulado", now, now));
        positions.save(new VehiclePosition(vehicleId, tripId,
            trip.getOriginLatitude(), trip.getOriginLongitude(), BigDecimal.ZERO,
            new BigDecimal("15.22"), BigDecimal.ZERO, now, now, "fleetops-simulator"));
        return mapper.toSummary(trip);
    }
}
