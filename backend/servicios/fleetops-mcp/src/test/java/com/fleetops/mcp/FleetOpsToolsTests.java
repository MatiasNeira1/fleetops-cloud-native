package com.fleetops.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleetops.mcp.api.FleetOpsApiClient;
import com.fleetops.mcp.api.FleetOpsApiException;
import com.fleetops.mcp.tools.FleetOpsTools;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class FleetOpsToolsTests {
    private HttpServer server;
    private FleetOpsTools tools;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicInteger alertStatus = new AtomicInteger(200);

    @BeforeEach
    void startRestApi() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/trips", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, 200, "[{\"id\":\"trip-001\",\"status\":\"IN_TRANSIT\"}]");
        });
        server.createContext("/api/v1/vehicles", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            respond(exchange, 200, "{\"vehicleId\":\"vehicle-001\",\"latitude\":-33.4}");
        });
        server.createContext("/api/v1/alerts", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            respond(exchange, alertStatus.get(), "{\"content\":[{\"id\":7}],\"totalElements\":1}");
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        tools = new FleetOpsTools(new FleetOpsApiClient(RestClient.create(baseUrl), RestClient.create(baseUrl)));
    }

    @AfterEach
    void stopRestApi() {
        server.stop(0);
    }

    @Test
    void readsRealTripEndpointAndBoundsResults() {
        Map<String, Object> result = tools.listActiveTrips("IN_TRANSIT", 1);
        assertEquals(1, result.get("returned"));
        assertFalse((boolean) result.get("truncated"));
        assertEquals("/api/v1/trips", lastPath.get());
        assertEquals("status=IN_TRANSIT", lastQuery.get());
        assertThrows(IllegalArgumentException.class, () -> tools.listActiveTrips(null, 51));
    }

    @Test
    void readsVehiclePositionAndPagedAlerts() {
        assertEquals("vehicle-001", tools.getVehiclePosition("vehicle-001").get("vehicleId"));
        assertEquals("/api/v1/vehicles/vehicle-001/position", lastPath.get());
        Map<String, Object> page = tools.listAlerts("trip-001", null, "LOW_FUEL", null, "ACTIVE", 2, 5);
        assertEquals(1, page.get("totalElements"));
        assertTrue(lastQuery.get().contains("page=2"));
        assertTrue(lastQuery.get().contains("size=5"));
        assertTrue(lastQuery.get().contains("status=ACTIVE"));
        assertThrows(IllegalArgumentException.class, () -> tools.getTrip("../admin"));
    }

    @Test
    void sanitizesDownstreamErrors() {
        for (int status : new int[] {401, 403, 404, 429, 500}) {
            alertStatus.set(status);
            FleetOpsApiException error = assertThrows(FleetOpsApiException.class, () -> tools.getAlert(7L));
            assertFalse(error.getMessage().contains("content"));
        }
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
