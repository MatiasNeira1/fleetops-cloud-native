package com.fleetops.alerta.infrastructure.persistence;

import com.fleetops.alerta.domain.model.Alert;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {
    boolean existsBySourceEventId(UUID sourceEventId);
}
