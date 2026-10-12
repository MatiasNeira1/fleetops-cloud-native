package com.fleetops.flota.api.controller;

import com.fleetops.flota.api.request.TelemetryRequest;
import com.fleetops.flota.api.response.TelemetryAcceptedResponse;
import com.fleetops.flota.application.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Aceptar telemetria y publicarla en RabbitMQ")
    public TelemetryAcceptedResponse receive(
        @Valid @RequestBody TelemetryRequest request
    ) {
        return telemetryService.accept(request);
    }
}
