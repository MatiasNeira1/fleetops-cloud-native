package com.fleetops.flota.infrastructure.persistence;

import com.fleetops.flota.domain.VehiclePosition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehiclePositionRepository extends JpaRepository<VehiclePosition, Long> {
    Optional<VehiclePosition> findFirstByVehicleIdOrderByRecordedAtDesc(String vehicleId);
}
