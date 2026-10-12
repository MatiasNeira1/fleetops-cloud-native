package com.fleetops.flota.api.controller;

import com.fleetops.flota.api.request.UpdateAlertStatusRequest;
import com.fleetops.flota.api.response.AlertResponse;
import com.fleetops.flota.application.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Gestion de alertas operativas")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado de una alerta")
    public AlertResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAlertStatusRequest request
    ) {
        return alertService.updateStatus(id, request);
    }
}
