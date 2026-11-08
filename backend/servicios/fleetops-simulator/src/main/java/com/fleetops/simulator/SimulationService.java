package com.fleetops.simulator;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class SimulationService {
    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);
    private final SimulatorProperties properties;
    private final FlotaApi api;
    private final RabbitLeaderLease lease;
    private final MeterRegistry metrics;
    private final Map<String, SimulationState> simulations = new LinkedHashMap<>();
    private boolean globalPaused;

    public SimulationService(SimulatorProperties properties, FlotaApi api,
                             RabbitLeaderLease lease, MeterRegistry metrics) {
        this.properties = properties;
        this.api = api;
        this.lease = lease;
        this.metrics = metrics;
    }

    public synchronized Status status() {
        List<TripStatus> trips = simulations.values().stream().map(state -> new TripStatus(
            state.tripId(), state.vehicleId(), state.paused(), state.arrived(),
            state.mode(), state.distanceKm(), state.speedKph(), state.fuelPercent(),
            state.failures())).toList();
        return new Status(properties.enabled(), lease.isLeader(), globalPaused, trips);
    }

    public synchronized Status startAll() {
        requireEnabledAndLeader();
        globalPaused = false;
        for (FlotaApi.Trip summary : api.activeTrips()) {
            if (simulations.size() >= properties.maxVehicles()) break;
            if (eligible(summary.status()) && !simulations.containsKey(summary.id())) {
                try { add(summary.id()); }
                catch (RuntimeException exception) {
                    log.warn("No se pudo iniciar simulacion tripId={} reason={}",
                        summary.id(), exception.getClass().getSimpleName());
                }
            }
        }
        return status();
    }

    public synchronized Status start(String tripId) {
        requireEnabledAndLeader();
        if (simulations.containsKey(tripId)) throw new IllegalStateException("Viaje ya iniciado");
        add(tripId);
        return status();
    }

    public synchronized Status startDemo() {
        requireEnabledAndLeader();
        FlotaApi.Trip trip = api.createDemo();
        if (trip == null) throw new IllegalStateException("Flota no devolvio el viaje de demostracion");
        if (!simulations.containsKey(trip.id())) add(trip.id());
        return status();
    }

    private void add(String tripId) {
        if (simulations.size() >= properties.maxVehicles()) {
            throw new IllegalStateException("Limite de vehiculos alcanzado");
        }
        FlotaApi.Trip trip = api.detail(tripId);
        if (trip == null || !eligible(trip.status())) {
            throw new IllegalStateException("Viaje no esta en curso");
        }
        if (simulations.values().stream().anyMatch(state -> state.vehicleId().equals(trip.vehicle().id()))) {
            throw new IllegalStateException("Vehiculo ya simulado en otro viaje");
        }
        simulations.put(tripId, new SimulationState(trip, properties.randomSeed(), properties));
    }

    public synchronized Status pauseAll() { globalPaused = true; return status(); }
    public synchronized Status resumeAll() {
        requireEnabledAndLeader();
        globalPaused = false;
        return status();
    }
    public synchronized Status stopAll() {
        simulations.clear();
        globalPaused = false;
        lease.release();
        return status();
    }
    public synchronized Status pause(String tripId) { state(tripId).pause(); return status(); }
    public synchronized Status resume(String tripId) {
        requireEnabledAndLeader();
        state(tripId).resume();
        return status();
    }
    public synchronized Status stop(String tripId) {
        simulations.remove(tripId);
        if (simulations.isEmpty()) lease.release();
        return status();
    }
    public synchronized Status restart(String tripId) {
        requireEnabledAndLeader();
        simulations.remove(tripId);
        add(tripId); // Se reanuda desde la ultima posicion y combustible persistidos en flota.
        return status();
    }
    public synchronized Status scenario(String tripId, Scenario scenario) {
        SimulationState state = state(tripId);
        switch (scenario) {
            case SPEED_LIMIT_EXCEEDED -> state.forceSpeed(properties.scenarioTicks());
            case STOP -> state.forceStop(properties.scenarioTicks());
            case PREPARE_LOW_FUEL -> state.prepareLowFuel();
        }
        return status();
    }
    public synchronized Status refuel(String tripId, double percent) {
        requireEnabledAndLeader();
        SimulationState state = state(tripId);
        var candidate = state.refuel(percent, Instant.now());
        send(state, candidate);
        return status();
    }

    @Scheduled(fixedDelayString = "${fleetops.simulator.interval:5s}")
    public synchronized void scheduledTick() {
        if (!properties.enabled() || globalPaused || simulations.isEmpty()) return;
        if (!lease.isLeader() && !lease.acquire()) {
            metrics.counter("fleetops_simulator_leader_rejections_total").increment();
            return;
        }
        for (SimulationState state : new ArrayList<>(simulations.values())) {
            if (state.paused()) continue;
            try {
                FlotaApi.Trip trip = api.detail(state.tripId());
                if (trip == null || !eligible(trip.status())
                    || (trip.lastPosition() != null && trip.lastPosition().tripId() != null
                        && !state.tripId().equals(trip.lastPosition().tripId()))) {
                    simulations.remove(state.tripId());
                    continue;
                }
                if (state.arrived()) {
                    api.complete(state.tripId());
                    simulations.remove(state.tripId());
                    metrics.counter("fleetops_simulator_completed_total").increment();
                    continue;
                }
                send(state, state.prepare(properties.interval().toMillis() / 1000.0,
                    Instant.now(), properties));
            } catch (RuntimeException exception) {
                state.failed();
                metrics.counter("fleetops_simulator_failures_total").increment();
                log.warn("Fallo de simulacion tripId={} reason={}",
                    state.tripId(), exception.getClass().getSimpleName());
                if (state.failures() >= properties.maxFailures()) {
                    state.pause();
                    metrics.counter("fleetops_simulator_paused_after_failures_total").increment();
                    log.warn("Simulacion pausada tras fallos consecutivos tripId={} count={}",
                        state.tripId(), state.failures());
                }
            }
        }
        if (simulations.isEmpty()) lease.release();
    }

    private void send(SimulationState state, SimulationState.Candidate candidate) {
        if (candidate == null) return;
        FlotaApi.TelemetryAccepted response = api.telemetry(candidate.telemetry());
        if (response == null || (!"ACCEPTED".equals(response.status())
            && !"DUPLICATE_IGNORED".equals(response.status()))) {
            throw new IllegalStateException("Telemetria no aceptada");
        }
        state.accepted();
        metrics.counter("fleetops_simulator_telemetry_accepted_total").increment();
    }

    private SimulationState state(String tripId) {
        SimulationState state = simulations.get(tripId);
        if (state == null) throw new IllegalArgumentException("Viaje no simulado");
        return state;
    }

    private void requireEnabledAndLeader() {
        if (!properties.enabled()) throw new IllegalStateException("Simulador desactivado por configuracion");
        if (!lease.acquire()) throw new IllegalStateException("Otra instancia mantiene el liderazgo o RabbitMQ no esta disponible");
    }

    private static boolean eligible(String status) {
        return "IN_TRANSIT".equals(status) || "DELAYED".equals(status) || "INCIDENT".equals(status);
    }

    public enum Scenario { SPEED_LIMIT_EXCEEDED, STOP, PREPARE_LOW_FUEL }
    public record TripStatus(String tripId, String vehicleId, boolean paused, boolean arrived,
                             String mode, double distanceKm, double speedKph,
                             double fuelPercent, int failures) {}
    public record Status(boolean enabled, boolean leader, boolean globalPaused,
                         List<TripStatus> trips) {}
}
