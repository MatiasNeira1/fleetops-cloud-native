package com.fleetops.flota.messaging.dto;

import jakarta.validation.constraints.NotBlank;

public record LogMessage(
    @NotBlank(message = "level es obligatorio")
    String level,

    @NotBlank(message = "message es obligatorio")
    String message,

    boolean simulateFailure
) {
}
