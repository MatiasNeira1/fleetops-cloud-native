package com.fleetops.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationStateTests {
    private final SimulatorProperties properties = new SimulatorProperties(true, Duration.ofSeconds(5),
        3, 5, 42, 65, 90, 105, 3, 0, 0, 6, 24, 0.16, 60, 15,
        "a".repeat(32), "b".repeat(32), "http://localhost:8080");

    @Test
    void gradualMovementPauseResumeAndFuelConsumption() {
        SimulationState state = new SimulationState(trip("IN_TRANSIT", 16), 42, properties);
        double original = state.distanceKm();
        var first = state.prepare(5, Instant.parse("2026-11-08T12:00:00Z"), properties);
        assertNotNull(first);
        assertTrue(first.distanceKm() > original);
        assertTrue(first.speedKph() <= 15);
        assertTrue(first.fuelPercent() < 16);
        state.accepted();
        double progressed = state.distanceKm();
        state.pause();
        assertEquals(null, state.prepare(5, Instant.now(), properties));
        assertEquals(progressed, state.distanceKm());
        state.resume();
        state.prepare(5, Instant.now(), properties);
        state.accepted();
        assertTrue(state.distanceKm() > progressed);
        assertTrue(state.fuelPercent() <= first.fuelPercent());
    }

    @Test
    void forcedSpeedRecoversAndLowFuelComesFromDistance() {
        SimulationState state = new SimulationState(trip("IN_TRANSIT", 16), 42, properties);
        state.forceSpeed(12);
        boolean speeding = false;
        for (int i = 0; i < 12; i++) {
            var candidate = state.prepare(5, Instant.now(), properties);
            speeding |= candidate.speedKph() > properties.speedLimitKph();
            state.accepted();
        }
        assertTrue(speeding);
        for (int i = 0; i < 10; i++) {
            state.prepare(5, Instant.now(), properties);
            state.accepted();
        }
        assertTrue(state.speedKph() <= properties.speedLimitKph());
        double before = state.fuelPercent();
        state.prepareLowFuel();
        for (int i = 0; i < 30 && state.fuelPercent() >= 15 && !state.arrived(); i++) {
            state.prepare(5, Instant.now(), properties);
            state.accepted();
        }
        assertTrue(state.fuelPercent() <= before);
        assertTrue(state.fuelPercent() >= 0);
    }

    @Test
    void retriesSameEventWithoutAdvancingTwice() {
        SimulationState state = new SimulationState(trip("IN_TRANSIT", 20), 42, properties);
        var first = state.prepare(5, Instant.now(), properties);
        state.failed();
        var retry = state.prepare(5, Instant.now(), properties);
        assertEquals(first.telemetry().eventId(), retry.telemetry().eventId());
        assertEquals(0, state.distanceKm());
        state.accepted();
        assertEquals(first.distanceKm(), state.distanceKm());
    }

    @Test
    void stopLastsConfiguredTicksAndRestartUsesPersistedFuelAndPosition() {
        SimulationState state = new SimulationState(trip("IN_TRANSIT", 14.5), 42, properties);
        state.forceStop(3);
        for (int tick = 0; tick < 3; tick++) {
            var candidate = state.prepare(5, Instant.now(), properties);
            assertEquals(0, candidate.speedKph());
            state.accepted();
        }
        state.prepare(5, Instant.now(), properties);
        state.accepted();
        assertTrue(state.speedKph() > 0);

        var original = trip("IN_TRANSIT", 14.5);
        var persisted = new FlotaApi.Position("vehicle-1", "trip-1", BigDecimal.ZERO,
            new BigDecimal("0.02"), new BigDecimal("40"), new BigDecimal("14.5"),
            new BigDecimal("102"), Instant.now(), "fleetops-simulator");
        var resumed = new FlotaApi.Trip(original.id(), original.origin(), original.destination(),
            original.vehicle(), original.status(), 20, original.totalDistanceKm(),
            new BigDecimal("2.2"), persisted, original.routeCoordinates(), original.routeGeometryType());
        SimulationState restarted = new SimulationState(resumed, 42, properties);
        assertTrue(restarted.distanceKm() > 2);
        assertEquals(14.5, restarted.fuelPercent());
        assertEquals(40, restarted.speedKph());
    }

    private static FlotaApi.Trip trip(String status, double fuel) {
        var origin = new FlotaApi.Place("A", BigDecimal.ZERO, BigDecimal.ZERO);
        var destination = new FlotaApi.Place("B", BigDecimal.ZERO, new BigDecimal("0.1"));
        var position = new FlotaApi.Position("vehicle-1", "trip-1", BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(fuel),
            new BigDecimal("100"), Instant.now(), "fleetops-simulator");
        return new FlotaApi.Trip("trip-1", origin, destination,
            new FlotaApi.Vehicle("vehicle-1", "AA-11"), status, 0,
            new BigDecimal("12"), BigDecimal.ZERO, position,
            List.of(List.of(BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(BigDecimal.ZERO, new BigDecimal("0.1"))), "SIMULATED_STRAIGHT_LINE");
    }
}
