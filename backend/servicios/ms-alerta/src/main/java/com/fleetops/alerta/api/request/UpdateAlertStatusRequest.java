package com.fleetops.alerta.api.request;

import com.fleetops.alerta.domain.model.AlertStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAlertStatusRequest(@NotNull AlertStatus status) {
}
