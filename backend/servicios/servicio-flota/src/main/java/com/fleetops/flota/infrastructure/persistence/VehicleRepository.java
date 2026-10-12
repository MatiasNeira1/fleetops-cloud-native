package com.fleetops.flota.infrastructure.persistence;

import com.fleetops.flota.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
}
