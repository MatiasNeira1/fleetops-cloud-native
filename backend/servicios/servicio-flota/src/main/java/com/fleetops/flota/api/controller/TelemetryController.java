package com.fleetops.flota.api.controller;

import com.fleetops.flota.api.request.TelemetryRequest;
import com.fleetops.flota.api.response.TelemetryAcceptedResponse;
import com.fleetops.flota.application.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "Telemetry", description = "Ingreso de telemetria simulada")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Aceptar telemetría y publicarla en RabbitMQ", description = "La respuesta 202 confirma la aceptación; la persistencia y las alertas se procesan de forma asíncrona.")
    @ApiResponses({@ApiResponse(responseCode = "202", description = "Telemetría aceptada"),
        @ApiResponse(responseCode = "400", description = "Telemetría inválida"),
        @ApiResponse(responseCode = "404", description = "Vehículo o viaje inexistente"),
        @ApiResponse(responseCode = "409", description = "Conflicto con el estado del viaje")})
    public TelemetryAcceptedResponse receive(
        @Valid @RequestBody TelemetryRequest request
    ) {
        return telemetryService.accept(request);
    }
}
