package com.fleetops.flota.api.request;

import com.fleetops.flota.domain.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateIncidentStatusRequest(
    @NotNull IncidentStatus status,
    String resolutionNote
) {
}
