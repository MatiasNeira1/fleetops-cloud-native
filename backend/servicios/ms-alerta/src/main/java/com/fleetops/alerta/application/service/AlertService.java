package com.fleetops.alerta.application.service;

import com.fleetops.alerta.api.response.AlertResponse;
import com.fleetops.alerta.domain.model.Alert;
import com.fleetops.alerta.domain.model.AlertSeverity;
import com.fleetops.alerta.domain.model.AlertStatus;
import com.fleetops.alerta.domain.model.AlertType;
import com.fleetops.alerta.infrastructure.messaging.AlertCreatedEvent;
import com.fleetops.alerta.infrastructure.persistence.AlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private final AlertRepository repository;

    public AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean create(AlertCreatedEvent event) {
        event.validate();
        AlertType type = AlertType.valueOf(event.alertType());
        AlertSeverity severity = AlertSeverity.valueOf(event.severity());
        if (repository.existsBySourceEventId(event.eventId())) return false;
        repository.saveAndFlush(new Alert(event.eventId(), event.alertId(), event.correlationId(), event.tripId(),
            event.vehicleId(), type, severity, event.title(), event.message(),
            event.measuredValue(), event.thresholdValue(), event.occurredAt()));
        return true;
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> list(String tripId, String vehicleId, AlertType type,
                                    AlertSeverity severity, AlertStatus status, Pageable pageable) {
        Specification<Alert> filter = (root, query, builder) -> builder.conjunction();
        if (tripId != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("tripId"), tripId));
        if (vehicleId != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("vehicleId"), vehicleId));
        if (type != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("type"), type));
        if (severity != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("severity"), severity));
        if (status != null) filter = filter.and((root, query, builder) -> builder.equal(root.get("status"), status));
        return repository.findAll(filter, pageable).map(AlertResponse::from);
    }

    @Transactional(readOnly = true)
    public AlertResponse get(Long id) {
        return repository.findById(id).map(AlertResponse::from)
            .orElseThrow(() -> new AlertNotFoundException(id));
    }

    @Transactional
    public AlertResponse updateStatus(Long id, AlertStatus status) {
        Alert alert = repository.findById(id).orElseThrow(() -> new AlertNotFoundException(id));
        alert.changeStatus(status);
        return AlertResponse.from(alert);
    }
}
