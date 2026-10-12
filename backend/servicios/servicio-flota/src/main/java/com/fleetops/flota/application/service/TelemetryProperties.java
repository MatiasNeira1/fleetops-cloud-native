package com.fleetops.flota.application.service;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fleetops.telemetry")
public record TelemetryProperties(
    BigDecimal maxSpeedKph,
    BigDecimal minFuelPercent
) {
}
