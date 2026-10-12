package com.fleetops.flota.api.controller;

import com.fleetops.flota.api.request.CreateIncidentRequest;
import com.fleetops.flota.api.request.UpdateIncidentStatusRequest;
import com.fleetops.flota.api.response.IncidentResponse;
import com.fleetops.flota.application.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Registro y gestion de incidentes")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @Operation(summary = "Registrar un incidente")
    public ResponseEntity<IncidentResponse> create(
        @Valid @RequestBody CreateIncidentRequest request
    ) {
        IncidentResponse response = incidentService.create(request);
        return ResponseEntity
            .created(URI.create("/api/v1/incidents/" + response.id()))
            .body(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar el estado de un incidente")
    public IncidentResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateIncidentStatusRequest request
    ) {
        return incidentService.updateStatus(id, request);
    }
}
