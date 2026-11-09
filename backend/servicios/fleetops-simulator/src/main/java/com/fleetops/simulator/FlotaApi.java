package com.fleetops.simulator;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FlotaApi {
    private final RestClient client;
    private final String serviceToken;

    public FlotaApi(SimulatorProperties properties) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder().baseUrl(properties.flotaBaseUrl())
            .requestFactory(factory).build();
        this.serviceToken = properties.serviceToken();
    }

    public List<Trip> activeTrips() {
        Trip[] result = client.get().uri("/api/v1/trips").retrieve().body(Trip[].class);
        return result == null ? List.of() : Arrays.asList(result);
    }

    public Trip detail(String id) {
        return client.get().uri("/api/v1/trips/{id}", id).retrieve().body(Trip.class);
    }

    public TelemetryAccepted telemetry(Telemetry request) {
        return client.post().uri("/api/v1/telemetry").contentType(MediaType.APPLICATION_JSON)
            .body(request).retrieve().body(TelemetryAccepted.class);
    }

    public Trip complete(String id) {
        if (serviceToken == null || serviceToken.length() < 32) {
            throw new IllegalStateException("SIMULATOR_SERVICE_TOKEN no configurado");
        }
        return client.post().uri("/api/v1/trips/{id}/complete", id)
            .headers(headers -> headers.setBearerAuth(serviceToken))
            .retrieve().body(Trip.class);
    }

    public Trip createDemo() {
        requireServiceToken();
        return client.post().uri("/api/v1/trips/demo")
            .headers(headers -> headers.setBearerAuth(serviceToken))
            .retrieve().body(Trip.class);
    }

    private void requireServiceToken() {
        if (serviceToken == null || serviceToken.length() < 32) {
            throw new IllegalStateException("SIMULATOR_SERVICE_TOKEN no configurado");
        }
    }

    public record Place(String name, BigDecimal latitude, BigDecimal longitude) {}
    public record Vehicle(String id, String plate) {}
    public record Position(String vehicleId, String tripId, BigDecimal latitude, BigDecimal longitude,
                           BigDecimal speedKph, BigDecimal fuelPercent, BigDecimal odometerKm,
                           Instant recordedAt, String source) {}
    public record Trip(String id, Place origin, Place destination, Vehicle vehicle, String status,
                       int progress, BigDecimal totalDistanceKm, BigDecimal travelledDistanceKm,
                       Position lastPosition, List<List<BigDecimal>> routeCoordinates,
                       String routeGeometryType) {}
    public record Telemetry(String eventId, String vehicleId, String tripId,
                            BigDecimal latitude, BigDecimal longitude, BigDecimal speedKph,
                            BigDecimal fuelPercent, BigDecimal odometerKm, Instant recordedAt,
                            String source) {}
    public record TelemetryAccepted(String eventId, String status, boolean duplicate) {}
}
