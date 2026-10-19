package com.fleetops.alerta.api.controller;

import com.fleetops.alerta.api.request.UpdateAlertStatusRequest;
import com.fleetops.alerta.api.response.AlertResponse;
import com.fleetops.alerta.application.service.AlertService;
import com.fleetops.alerta.domain.model.AlertSeverity;
import com.fleetops.alerta.domain.model.AlertStatus;
import com.fleetops.alerta.domain.model.AlertType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alerts", description = "Consulta y gestión de alertas")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping("/alerts")
    @Operation(summary = "Listar alertas")
    public Page<AlertResponse> list(@RequestParam(required = false) String tripId,
                                    @RequestParam(required = false) String vehicleId,
                                    @RequestParam(required = false) AlertType type,
                                    @RequestParam(required = false) AlertSeverity severity,
                                    @RequestParam(required = false) AlertStatus status,
                                    @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(tripId, vehicleId, type, severity, status, pageable);
    }

    @GetMapping("/alerts/{id}")
    @Operation(summary = "Obtener alerta")
    public AlertResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/trips/{tripId}/alerts")
    @Operation(summary = "Listar alertas de un viaje")
    public Page<AlertResponse> byTrip(@PathVariable String tripId,
                                      @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(tripId, null, null, null, null, pageable);
    }

    @PatchMapping("/alerts/{id}/status")
    @Operation(summary = "Cambiar estado de alerta")
    public AlertResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateAlertStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
