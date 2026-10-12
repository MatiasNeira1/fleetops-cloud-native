package com.fleetops.flota.infrastructure.persistence;

import com.fleetops.flota.domain.TelemetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryEventRepository extends JpaRepository<TelemetryEvent, String> {
}
