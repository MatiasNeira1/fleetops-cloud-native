package com.fleetops.flota.application.service;

import com.fleetops.flota.api.error.ConflictException;
import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.request.UpdateAlertStatusRequest;
import com.fleetops.flota.api.response.AlertResponse;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.infrastructure.persistence.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final FleetOpsMapper mapper;

    public AlertService(AlertRepository alertRepository, FleetOpsMapper mapper) {
        this.alertRepository = alertRepository;
        this.mapper = mapper;
    }

    @Transactional
    public AlertResponse updateStatus(Long id, UpdateAlertStatusRequest request) {
        var alert = alertRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No existe la alerta " + id));
        if (request.status() != AlertStatus.RESOLVED) {
            throw new ConflictException("Solo se permite resolver alertas desde la API operativa");
        }
        alert.resolve();
        return mapper.toAlert(alert);
    }
}
