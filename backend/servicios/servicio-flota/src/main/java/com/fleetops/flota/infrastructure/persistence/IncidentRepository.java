package com.fleetops.flota.infrastructure.persistence;

import com.fleetops.flota.domain.Incident;
import com.fleetops.flota.domain.IncidentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByTripIdAndStatus(String tripId, IncidentStatus status);
}
