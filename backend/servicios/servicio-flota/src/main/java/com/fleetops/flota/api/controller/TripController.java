package com.fleetops.flota.api.controller;

import com.fleetops.flota.api.response.AlertResponse;
import com.fleetops.flota.api.response.TripDetailResponse;
import com.fleetops.flota.api.response.TripSummaryResponse;
import com.fleetops.flota.application.service.TripQueryService;
import com.fleetops.flota.application.service.TripCompletionService;
import com.fleetops.flota.application.service.DemoTripService;
import com.fleetops.flota.domain.AlertSeverity;
import com.fleetops.flota.domain.AlertStatus;
import com.fleetops.flota.domain.TripStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trips")
@Tag(name = "Trips", description = "Consulta de viajes activos y sus alertas")
public class TripController {

    private final TripQueryService tripQueryService;
    private final TripCompletionService completionService;
    private final DemoTripService demoTripService;

    public TripController(TripQueryService tripQueryService, TripCompletionService completionService,
                          DemoTripService demoTripService) {
        this.tripQueryService = tripQueryService;
        this.completionService = completionService;
        this.demoTripService = demoTripService;
    }

    @GetMapping
    @Operation(summary = "Listar viajes activos", description = "Devuelve viajes no completados ni cancelados; status filtra por estado cuando se indica.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Viajes encontrados"),
        @ApiResponse(responseCode = "400", description = "Filtro de estado inválido")})
    public List<TripSummaryResponse> activeTrips(
        @Parameter(description = "Filtro opcional por estado del viaje")
        @RequestParam(required = false) TripStatus status
    ) {
        return tripQueryService.activeTrips(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un viaje", description = "Incluye ruta, última posición, alertas y línea de tiempo.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Viaje encontrado"),
        @ApiResponse(responseCode = "404", description = "Viaje inexistente")})
    public TripDetailResponse tripDetail(@PathVariable String id) {
        return tripQueryService.tripDetail(id);
    }

    @GetMapping("/{id}/alerts")
    @Operation(summary = "Obtener alertas asociadas a un viaje", description = "Permite filtrar por estado y severidad; devuelve una lista vacía si no hay alertas.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Alertas del viaje"),
        @ApiResponse(responseCode = "400", description = "Filtro inválido"),
        @ApiResponse(responseCode = "404", description = "Viaje inexistente")})
    public List<AlertResponse> tripAlerts(
        @PathVariable String id,
        @RequestParam(required = false) AlertStatus status,
        @RequestParam(required = false) AlertSeverity severity
    ) {
        return tripQueryService.alerts(id, status, severity);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Completar viaje tras llegada de telemetria simulada")
    public TripSummaryResponse complete(@PathVariable String id) {
        return completionService.complete(id);
    }

    @PostMapping("/demo")
    @Operation(summary = "Crear o recuperar viaje de demostracion simulada")
    public TripSummaryResponse createDemo() {
        return demoTripService.create();
    }
}
