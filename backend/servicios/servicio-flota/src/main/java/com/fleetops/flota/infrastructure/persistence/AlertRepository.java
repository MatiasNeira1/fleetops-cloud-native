package com.fleetops.flota.infrastructure.persistence;

import com.fleetops.flota.domain.Alert;
import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.domain.AlertType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByTripIdOrderByDetectedAtDesc(String tripId);
    List<Alert> findByTripIdAndStatusOrderByDetectedAtDesc(String tripId, AlertStatus status);
    List<Alert> findByTripIdAndSeverityOrderByDetectedAtDesc(String tripId, AlertSeverity severity);
    boolean existsByVehicleIdAndTypeAndStatus(String vehicleId, AlertType type, AlertStatus status);
}
