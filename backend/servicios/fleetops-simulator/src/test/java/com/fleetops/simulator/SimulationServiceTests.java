package com.fleetops.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationServiceTests {
    private final SimulatorProperties properties = new SimulatorProperties(true, Duration.ofSeconds(5),
        3, 3, 42, 65, 90, 105, 3, 0, 0, 6, 24, 0.16, 60, 15,
        "a".repeat(32), "b".repeat(32), "http://localhost:8080");

    @Test
    void onlyLeaderCanStartAndSameVehicleCannotRunTwice() {
        FlotaApi api = mock(FlotaApi.class);
        RabbitLeaderLease lease = mock(RabbitLeaderLease.class);
        SimulationService service = new SimulationService(properties, api, lease, new SimpleMeterRegistry());
        assertThrows(IllegalStateException.class, () -> service.start("trip-1"));
        when(lease.acquire()).thenReturn(true);
        when(api.detail("trip-1")).thenReturn(trip("trip-1", "IN_TRANSIT", "vehicle-1"));
        when(api.detail("trip-2")).thenReturn(trip("trip-2", "IN_TRANSIT", "vehicle-1"));
        service.start("trip-1");
        assertThrows(IllegalStateException.class, () -> service.start("trip-2"));
        assertEquals(1, service.status().trips().size());
    }

    @Test
    void excludesScheduledAndRetainsProgressWhilePaused() {
        FlotaApi api = mock(FlotaApi.class);
        RabbitLeaderLease lease = mock(RabbitLeaderLease.class);
        when(lease.acquire()).thenReturn(true);
        when(lease.isLeader()).thenReturn(true);
        when(api.detail("trip-1")).thenReturn(trip("trip-1", "IN_TRANSIT", "vehicle-1"));
        when(api.detail("scheduled")).thenReturn(trip("scheduled", "SCHEDULED", "vehicle-2"));
        SimulationService service = new SimulationService(properties, api, lease, new SimpleMeterRegistry());
        assertThrows(IllegalStateException.class, () -> service.start("scheduled"));
        service.start("trip-1");
        service.pause("trip-1");
        service.scheduledTick();
        assertEquals(0, service.status().trips().getFirst().distanceKm());
        service.resume("trip-1");
        when(api.telemetry(org.mockito.ArgumentMatchers.any())).thenReturn(
            new FlotaApi.TelemetryAccepted("id", "ACCEPTED", false));
        service.scheduledTick();
        verify(api).telemetry(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cancellationStopsTripAndFailureDoesNotBlockAnotherVehicle() {
        FlotaApi api = mock(FlotaApi.class);
        RabbitLeaderLease lease = mock(RabbitLeaderLease.class);
        when(lease.acquire()).thenReturn(true);
        when(lease.isLeader()).thenReturn(true);
        when(api.detail("cancelled")).thenReturn(trip("cancelled", "IN_TRANSIT", "vehicle-1"),
            trip("cancelled", "CANCELLED", "vehicle-1"));
        when(api.detail("broken")).thenReturn(trip("broken", "IN_TRANSIT", "vehicle-2"));
        when(api.detail("healthy")).thenReturn(trip("healthy", "IN_TRANSIT", "vehicle-3"));
        when(api.telemetry(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            FlotaApi.Telemetry request = invocation.getArgument(0);
            if (request.tripId().equals("broken")) throw new IllegalStateException("upstream");
            return new FlotaApi.TelemetryAccepted(request.eventId(), "ACCEPTED", false);
        });
        SimulationService service = new SimulationService(properties, api, lease, new SimpleMeterRegistry());
        service.start("cancelled");
        service.start("broken");
        service.start("healthy");
        service.scheduledTick();
        assertEquals(2, service.status().trips().size());
        assertEquals(1, service.status().trips().getFirst().failures());
        assertEquals(0, service.status().trips().get(1).failures());
        assertTrue(service.status().trips().get(1).distanceKm() > 0);
        verify(api, never()).complete("cancelled");
    }

    @Test
    void pausesOnlyFailingTripAfterBoundedRetries() {
        FlotaApi api = mock(FlotaApi.class);
        RabbitLeaderLease lease = mock(RabbitLeaderLease.class);
        when(lease.acquire()).thenReturn(true);
        when(lease.isLeader()).thenReturn(true);
        when(api.detail("broken")).thenReturn(trip("broken", "IN_TRANSIT", "vehicle-1"));
        when(api.telemetry(org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("upstream"));
        SimulationService service = new SimulationService(properties, api, lease, new SimpleMeterRegistry());
        service.start("broken");
        for (int i = 0; i < 5; i++) service.scheduledTick();
        assertTrue(service.status().trips().getFirst().paused());
        assertEquals(3, service.status().trips().getFirst().failures());
        service.resume("broken");
        assertEquals(0, service.status().trips().getFirst().failures());
    }

    private static FlotaApi.Trip trip(String id, String status, String vehicle) {
        var origin = new FlotaApi.Place("A", BigDecimal.ZERO, BigDecimal.ZERO);
        var destination = new FlotaApi.Place("B", BigDecimal.ZERO, new BigDecimal("0.1"));
        var position = new FlotaApi.Position(vehicle, id, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30"),
            new BigDecimal("100"), Instant.now(), "fleetops-simulator");
        return new FlotaApi.Trip(id, origin, destination, new FlotaApi.Vehicle(vehicle, "AA"),
            status, 0, new BigDecimal("12"), BigDecimal.ZERO, position,
            List.of(List.of(BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(BigDecimal.ZERO, new BigDecimal("0.1"))), "SIMULATED_STRAIGHT_LINE");
    }
}
