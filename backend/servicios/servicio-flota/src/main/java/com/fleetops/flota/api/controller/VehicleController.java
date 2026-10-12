package com.fleetops.flota.api.controller;

import com.fleetops.flota.api.response.VehiclePositionResponse;
import com.fleetops.flota.application.service.VehiclePositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Consulta de vehiculos")
public class VehicleController {

    private final VehiclePositionService vehiclePositionService;

    public VehicleController(VehiclePositionService vehiclePositionService) {
        this.vehiclePositionService = vehiclePositionService;
    }

    @GetMapping("/{id}/position")
    @Operation(summary = "Obtener la ultima posicion conocida de un vehiculo")
    public VehiclePositionResponse latestPosition(@PathVariable String id) {
        return vehiclePositionService.latestPosition(id);
    }
}
