package com.fleetops.flota.api.request;

import com.fleetops.flota.domain.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAlertStatusRequest(
    @NotNull AlertStatus status
) {
}
