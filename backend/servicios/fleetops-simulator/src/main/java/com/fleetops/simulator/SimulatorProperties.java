package com.fleetops.simulator;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fleetops.simulator")
public record SimulatorProperties(
    boolean enabled, Duration interval, int maxVehicles, int maxFailures, long randomSeed,
    double normalSpeedKph, double speedLimitKph, double speedingKph,
    double accelerationKphPerSecond, double speedingProbability, double stopProbability,
    int scenarioTicks, int cooldownTicks, double fuelPercentPerKm,
    double initialFuelPercent, double lowFuelThresholdPercent,
    String adminToken, String serviceToken, String flotaBaseUrl
) {
    public SimulatorProperties {
        if (interval == null || interval.isNegative() || interval.isZero()
            || interval.compareTo(Duration.ofSeconds(1)) < 0 || maxVehicles < 1 || maxVehicles > 20
            || maxFailures < 1 || maxFailures > 100
            || normalSpeedKph <= 0 || speedLimitKph <= normalSpeedKph
            || speedingKph <= speedLimitKph || accelerationKphPerSecond <= 0
            || speedingProbability < 0 || speedingProbability > 1
            || stopProbability < 0 || stopProbability > 1
            || speedingProbability + stopProbability >= 1
            || scenarioTicks < 1 || cooldownTicks < 0 || fuelPercentPerKm <= 0
            || initialFuelPercent < 0 || initialFuelPercent > 100
            || lowFuelThresholdPercent <= 0 || lowFuelThresholdPercent >= 100) {
            throw new IllegalArgumentException("Configuracion del simulador invalida");
        }
    }
}
