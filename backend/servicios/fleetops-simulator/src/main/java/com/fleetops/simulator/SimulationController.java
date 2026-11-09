package com.fleetops.simulator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {
    private final SimulationService service;

    public SimulationController(SimulationService service) { this.service = service; }

    @GetMapping
    public SimulationService.Status status() { return service.status(); }

    @PostMapping("/start")
    public SimulationService.Status startAll() { return service.startAll(); }
    @PostMapping("/demo/start")
    public SimulationService.Status startDemo() { return service.startDemo(); }
    @PostMapping("/pause")
    public SimulationService.Status pauseAll() { return service.pauseAll(); }
    @PostMapping("/resume")
    public SimulationService.Status resumeAll() { return service.resumeAll(); }
    @PostMapping("/stop")
    public SimulationService.Status stopAll() { return service.stopAll(); }

    @PostMapping("/{tripId}/start")
    public SimulationService.Status start(@PathVariable String tripId) { return service.start(tripId); }
    @PostMapping("/{tripId}/pause")
    public SimulationService.Status pause(@PathVariable String tripId) { return service.pause(tripId); }
    @PostMapping("/{tripId}/resume")
    public SimulationService.Status resume(@PathVariable String tripId) { return service.resume(tripId); }
    @PostMapping("/{tripId}/stop")
    public SimulationService.Status stop(@PathVariable String tripId) { return service.stop(tripId); }
    @PostMapping("/{tripId}/restart")
    public SimulationService.Status restart(@PathVariable String tripId) { return service.restart(tripId); }

    @PostMapping("/{tripId}/scenario")
    public SimulationService.Status scenario(@PathVariable String tripId,
        @Valid @RequestBody ScenarioRequest request) {
        return service.scenario(tripId, request.scenario());
    }
    @PostMapping("/{tripId}/refuel")
    public SimulationService.Status refuel(@PathVariable String tripId,
        @Valid @RequestBody RefuelRequest request) {
        return service.refuel(tripId, request.percent());
    }

    public record ScenarioRequest(@NotNull SimulationService.Scenario scenario) {}
    public record RefuelRequest(@DecimalMin("0.01") @DecimalMax("100") double percent) {}

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest() { return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST); }
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict() { return ProblemDetail.forStatus(HttpStatus.CONFLICT); }
    @ExceptionHandler(RestClientException.class)
    ProblemDetail upstream() { return ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY); }
}
