package com.fleetops.simulator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.SplittableRandom;
import java.util.UUID;

public final class SimulationState {
    private final String tripId;
    private final String vehicleId;
    private final RoutePath path;
    private final SplittableRandom random;
    private double distanceKm;
    private double speedKph;
    private double fuelPercent;
    private double odometerKm;
    private int speedTicks;
    private int stopTicks;
    private int cooldownTicks;
    private double burnMultiplier = 1;
    private boolean paused;
    private boolean arrived;
    private int failures;
    private Candidate pending;

    public SimulationState(FlotaApi.Trip trip, long seed, SimulatorProperties properties) {
        this.tripId = trip.id();
        this.vehicleId = trip.vehicle().id();
        this.path = RoutePath.from(trip);
        this.random = new SplittableRandom(seed ^ trip.id().hashCode());
        FlotaApi.Position previous = trip.lastPosition();
        this.distanceKm = previous == null ? path.lengthKm() * trip.progress() / 100.0
            : Math.max(path.lengthKm() * trip.progress() / 100.0,
                path.project(new RoutePath.Point(previous.latitude().doubleValue(),
                    previous.longitude().doubleValue())));
        this.speedKph = previous == null ? 0 : previous.speedKph().doubleValue();
        this.fuelPercent = previous == null ? properties.initialFuelPercent()
            : previous.fuelPercent().doubleValue();
        this.odometerKm = previous == null ? 0 : previous.odometerKm().doubleValue();
    }

    public Candidate prepare(double elapsedSeconds, Instant now, SimulatorProperties properties) {
        if (pending != null) return pending;
        if (arrived || paused) return null;
        int newSpeedTicks = Math.max(0, speedTicks - 1);
        int newStopTicks = Math.max(0, stopTicks - 1);
        int newCooldownTicks = Math.max(0, cooldownTicks - 1);
        if (speedTicks == 0 && stopTicks == 0 && newCooldownTicks == 0) {
            double roll = random.nextDouble();
            if (roll < properties.speedingProbability()) {
                newSpeedTicks = properties.scenarioTicks();
            } else if (roll < properties.speedingProbability() + properties.stopProbability()) {
                newStopTicks = properties.scenarioTicks();
            }
            if (newSpeedTicks > 0 || newStopTicks > 0) newCooldownTicks = properties.cooldownTicks();
        }
        double target = stopTicks > 0 || newStopTicks > 0 || fuelPercent <= 0 ? 0
            : speedTicks > 0 || newSpeedTicks > 0
                ? properties.speedingKph() : properties.normalSpeedKph();
        double delta = properties.accelerationKphPerSecond() * elapsedSeconds;
        double newSpeed = speedKph + Math.max(-delta, Math.min(delta, target - speedKph));
        double increment = (speedKph + newSpeed) / 2 * elapsedSeconds / 3600;
        double newDistance = Math.min(path.lengthKm(), distanceKm + Math.max(0, increment));
        double travelled = newDistance - distanceKm;
        double newFuel = Math.max(0, fuelPercent - travelled
            * properties.fuelPercentPerKm() * burnMultiplier);
        boolean destination = newDistance >= path.lengthKm() - 0.000001;
        if (destination) newSpeed = 0;
        RoutePath.Point point = path.at(newDistance);
        var telemetry = new FlotaApi.Telemetry(
            "sim-" + UUID.randomUUID(), vehicleId, tripId,
            decimal(point.latitude(), 7), decimal(point.longitude(), 7),
            decimal(newSpeed, 2), decimal(newFuel, 2),
            decimal(odometerKm + travelled, 2), now, "fleetops-simulator");
        pending = new Candidate(telemetry, newDistance, newSpeed, newFuel,
            odometerKm + travelled, newSpeedTicks, newStopTicks, newCooldownTicks, destination);
        return pending;
    }

    public void accepted() {
        if (pending == null) return;
        distanceKm = pending.distanceKm();
        speedKph = pending.speedKph();
        fuelPercent = pending.fuelPercent();
        odometerKm = pending.odometerKm();
        speedTicks = pending.speedTicks();
        stopTicks = pending.stopTicks();
        cooldownTicks = pending.cooldownTicks();
        arrived = pending.arrived();
        if (fuelPercent < 15) burnMultiplier = 1;
        pending = null;
        failures = 0;
    }

    public void failed() { failures++; }
    public int failures() { return failures; }
    public void pause() { paused = true; }
    public void resume() { paused = false; failures = 0; }
    public boolean paused() { return paused; }
    public boolean arrived() { return arrived; }
    public String tripId() { return tripId; }
    public String vehicleId() { return vehicleId; }
    public double distanceKm() { return distanceKm; }
    public double speedKph() { return speedKph; }
    public double fuelPercent() { return fuelPercent; }
    public String mode() { return stopTicks > 0 ? "STOPPED" : speedTicks > 0 ? "SPEED_LIMIT_EXCEEDED" : "NORMAL"; }

    public void forceSpeed(int ticks) {
        if (pending != null) throw new IllegalStateException("Hay telemetria pendiente");
        speedTicks = ticks;
        stopTicks = 0;
    }
    public void forceStop(int ticks) {
        if (pending != null) throw new IllegalStateException("Hay telemetria pendiente");
        stopTicks = ticks;
        speedTicks = 0;
    }
    public void prepareLowFuel() { burnMultiplier = 10; }

    public Candidate refuel(double percent, Instant now) {
        if (pending != null) throw new IllegalStateException("Hay telemetria pendiente");
        if (percent <= 0 || fuelPercent + percent > 100) {
            throw new IllegalArgumentException("Recarga fuera del rango 0..100");
        }
        RoutePath.Point point = path.at(distanceKm);
        var telemetry = new FlotaApi.Telemetry("sim-" + UUID.randomUUID(), vehicleId, tripId,
            decimal(point.latitude(), 7), decimal(point.longitude(), 7),
            decimal(0, 2), decimal(fuelPercent + percent, 2),
            decimal(odometerKm, 2), now, "fleetops-simulator");
        pending = new Candidate(telemetry, distanceKm, 0, fuelPercent + percent,
            odometerKm, 0, 0, cooldownTicks, arrived);
        return pending;
    }

    public record Candidate(FlotaApi.Telemetry telemetry, double distanceKm, double speedKph,
                            double fuelPercent, double odometerKm, int speedTicks,
                            int stopTicks, int cooldownTicks, boolean arrived) {}

    private static BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
