package com.fleetops.flota.infrastructure.config;

import com.fleetops.flota.domain.Alert;
import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.domain.AlertType;
import com.fleetops.flota.domain.Trip;
import com.fleetops.flota.domain.TripStatus;
import com.fleetops.flota.domain.Vehicle;
import com.fleetops.flota.domain.VehiclePosition;
import com.fleetops.flota.infrastructure.persistence.AlertRepository;
import com.fleetops.flota.infrastructure.persistence.TripRepository;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev"})
@ConditionalOnProperty(
    prefix = "fleetops.demo-data",
    name = "enabled",
    havingValue = "true"
)
public class DemoDataInitializer implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final VehiclePositionRepository positionRepository;
    private final AlertRepository alertRepository;

    public DemoDataInitializer(
        VehicleRepository vehicleRepository,
        TripRepository tripRepository,
        VehiclePositionRepository positionRepository,
        AlertRepository alertRepository
    ) {
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.positionRepository = positionRepository;
        this.alertRepository = alertRepository;
    }

    @Override
    public void run(String... args) {
        if (tripRepository.count() > 0) {
            return;
        }
        seedVehicles();
        seedTrips();
        seedPositions();
        seedAlerts();
    }

    private void seedVehicles() {
        vehicleRepository.saveAll(List.of(
            new Vehicle("vehicle-001", "FH-82-19"),
            new Vehicle("vehicle-002", "LK-40-22"),
            new Vehicle("vehicle-003", "PR-75-30"),
            new Vehicle("vehicle-004", "ZX-11-48")
        ));
    }

    private void seedTrips() {
        Instant now = Instant.now();
        tripRepository.saveAll(List.of(
            trip(
                "trip-001", "Santiago", "-33.4489000", "-70.6693000",
                "Valparaiso", "-33.0472000", "-71.6127000",
                TripStatus.IN_TRANSIT, 58, "120.00", "69.60",
                "vehicle-001", "FH-82-19", "driver-001", "Carlos Soto",
                now.minusSeconds(5400), now.plusSeconds(3600)
            ),
            trip(
                "trip-002", "Santiago", "-33.4489000", "-70.6693000",
                "Rancagua", "-34.1708000", "-70.7444000",
                TripStatus.DELAYED, 42, "87.00", "36.54",
                "vehicle-002", "LK-40-22", "driver-002", "Mariana Rojas",
                now.minusSeconds(4200), now.plusSeconds(5400)
            ),
            trip(
                "trip-003", "Santiago", "-33.4489000", "-70.6693000",
                "San Antonio", "-33.5922000", "-71.6217000",
                TripStatus.INCIDENT, 31, "108.00", "33.48",
                "vehicle-003", "PR-75-30", "driver-003", "Ignacio Perez",
                now.minusSeconds(3600), now.plusSeconds(7200)
            ),
            trip(
                "trip-004", "Concepcion", "-36.8201000", "-73.0444000",
                "Los Angeles", "-37.4697000", "-72.3537000",
                TripStatus.SCHEDULED, 0, "130.00", "0.00",
                "vehicle-004", "ZX-11-48", "driver-004", "Valentina Munoz",
                now.plusSeconds(3600), now.plusSeconds(14400)
            )
        ));
    }

    private Trip trip(
        String id,
        String origin,
        String originLat,
        String originLon,
        String destination,
        String destinationLat,
        String destinationLon,
        TripStatus status,
        int progress,
        String totalDistance,
        String travelledDistance,
        String vehicleId,
        String plate,
        String driverId,
        String driverName,
        Instant departure,
        Instant eta
    ) {
        Instant now = Instant.now();
        return new Trip(
            id,
            origin,
            new BigDecimal(originLat),
            new BigDecimal(originLon),
            destination,
            new BigDecimal(destinationLat),
            new BigDecimal(destinationLon),
            departure,
            eta,
            status,
            progress,
            new BigDecimal(totalDistance),
            new BigDecimal(travelledDistance),
            vehicleId,
            plate,
            driverId,
            driverName,
            now,
            now
        );
    }

    private void seedPositions() {
        Instant now = Instant.now();
        positionRepository.saveAll(List.of(
            position("vehicle-001", "trip-001", "-33.2321000", "-71.0201000", "82.50", "44.20", "128540.70", now),
            position("vehicle-002", "trip-002", "-33.8976000", "-70.7358000", "65.00", "61.00", "83610.40", now),
            position("vehicle-003", "trip-003", "-33.5102000", "-71.2430000", "0.00", "12.00", "151420.00", now)
        ));
    }

    private VehiclePosition position(
        String vehicleId,
        String tripId,
        String latitude,
        String longitude,
        String speed,
        String fuel,
        String odometer,
        Instant recordedAt
    ) {
        return new VehiclePosition(
            vehicleId,
            tripId,
            new BigDecimal(latitude),
            new BigDecimal(longitude),
            new BigDecimal(speed),
            new BigDecimal(fuel),
            new BigDecimal(odometer),
            recordedAt,
            Instant.now()
        );
    }

    private void seedAlerts() {
        Instant now = Instant.now();
        alertRepository.saveAll(List.of(
            new Alert(
                "trip-002",
                "vehicle-002",
                AlertType.PROLONGED_STOP,
                AlertSeverity.WARNING,
                "Retraso detectado",
                "El viaje presenta una demora sobre la planificacion",
                AlertStatus.OPEN,
                now.minusSeconds(900)
            ),
            new Alert(
                "trip-003",
                "vehicle-003",
                AlertType.LOW_FUEL,
                AlertSeverity.CRITICAL,
                "Combustible bajo",
                "El vehiculo reporta combustible bajo el umbral configurado",
                AlertStatus.OPEN,
                now.minusSeconds(600)
            )
        ));
    }
}
