package com.fleetops.flota.application.service;

import com.fleetops.flota.api.error.ResourceNotFoundException;
import com.fleetops.flota.api.mapper.FleetOpsMapper;
import com.fleetops.flota.api.response.VehiclePositionResponse;
import com.fleetops.flota.infrastructure.persistence.VehiclePositionRepository;
import com.fleetops.flota.infrastructure.persistence.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehiclePositionService {

    private final VehicleRepository vehicleRepository;
    private final VehiclePositionRepository positionRepository;
    private final FleetOpsMapper mapper;

    public VehiclePositionService(
        VehicleRepository vehicleRepository,
        VehiclePositionRepository positionRepository,
        FleetOpsMapper mapper
    ) {
        this.vehicleRepository = vehicleRepository;
        this.positionRepository = positionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public VehiclePositionResponse latestPosition(String vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("No existe el vehiculo " + vehicleId);
        }
        return positionRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicleId)
            .map(mapper::toPosition)
            .orElseThrow(() -> new ResourceNotFoundException(
                "El vehiculo " + vehicleId + " todavia no tiene posicion registrada"
            ));
    }
}
