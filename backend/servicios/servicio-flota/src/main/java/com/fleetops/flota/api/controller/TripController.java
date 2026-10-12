package com.fleetops.flota.api.controller;

import com.fleetops.flota.api.response.AlertResponse;
import com.fleetops.flota.api.response.TripDetailResponse;
import com.fleetops.flota.api.response.TripSummaryResponse;
import com.fleetops.flota.application.service.TripQueryService;
import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.domain.TripStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trips", description = "Consulta de viajes activos y sus alertas")
public class TripController {

    private final TripQueryService tripQueryService;

    public TripController(TripQueryService tripQueryService) {
        this.tripQueryService = tripQueryService;
    }

    @GetMapping
    @Operation(summary = "Listar viajes activos")
    public List<TripSummaryResponse> activeTrips(
        @Parameter(description = "Filtro opcional por estado del viaje")
        @RequestParam(required = false) TripStatus status
    ) {
        return tripQueryService.activeTrips(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un viaje")
    public TripDetailResponse tripDetail(@PathVariable String id) {
        return tripQueryService.tripDetail(id);
    }

    @GetMapping("/{id}/alerts")
    @Operation(summary = "Obtener alertas asociadas a un viaje")
    public List<AlertResponse> tripAlerts(
        @PathVariable String id,
        @RequestParam(required = false) AlertStatus status,
        @RequestParam(required = false) AlertSeverity severity
    ) {
        return tripQueryService.alerts(id, status, severity);
    }
}
