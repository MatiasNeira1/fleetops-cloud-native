package com.fleetops.mcp.tools;

import com.fleetops.mcp.api.FleetOpsApiClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class FleetOpsTools {
    private static final Set<String> TRIP_STATUSES = Set.of("SCHEDULED", "IN_TRANSIT", "DELAYED", "INCIDENT", "COMPLETED", "CANCELLED");
    private static final Set<String> ALERT_TYPES = Set.of("SPEED_LIMIT_EXCEEDED", "LOW_FUEL");
    private static final Set<String> SEVERITIES = Set.of("INFO", "WARNING", "CRITICAL");
    private static final Set<String> ALERT_STATUSES = Set.of("ACTIVE", "ACKNOWLEDGED", "RESOLVED");
    private final FleetOpsApiClient api;

    public FleetOpsTools(FleetOpsApiClient api) {
        this.api = api;
    }

    @McpTool(name = "list_active_trips", description = "Consulta viajes activos de FleetOps. El backend no pagina esta ruta; se limita la cantidad devuelta a 50.",
        generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> listActiveTrips(
        @McpToolParam(description = "Estado: SCHEDULED, IN_TRANSIT, DELAYED, INCIDENT, COMPLETED o CANCELLED", required = false) String status,
        @McpToolParam(description = "Máximo de viajes devueltos, entre 1 y 50; predeterminado 20", required = false) Integer limit) {
        Map<String, Object> query = new HashMap<>();
        addEnum(query, "status", status, TRIP_STATUSES);
        int max = bounded(limit, 20, 50, "limit");
        List<Map<String, Object>> trips = api.flotaList("/api/v1/trips", query);
        return Map.of("items", trips.subList(0, Math.min(max, trips.size())),
            "returned", Math.min(max, trips.size()), "truncated", trips.size() > max);
    }

    @McpTool(name = "get_trip", description = "Obtiene el detalle de un viaje por su ID, incluidos vehículo, conductor, ruta y alertas.",
        generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> getTrip(@McpToolParam(description = "ID del viaje") String tripId) {
        return api.flotaObject("/api/v1/trips/" + validId(tripId));
    }

    @McpTool(name = "get_vehicle_position", description = "Obtiene la última posición conocida de un vehículo por su ID.",
        generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> getVehiclePosition(@McpToolParam(description = "ID del vehículo") String vehicleId) {
        return api.flotaObject("/api/v1/vehicles/" + validId(vehicleId) + "/position");
    }

    @McpTool(name = "list_alerts", description = "Consulta alertas de ms-alerta con filtros y paginación. Devuelve la página y sus metadatos reales.",
        generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> listAlerts(
        @McpToolParam(description = "ID del viaje", required = false) String tripId,
        @McpToolParam(description = "ID del vehículo", required = false) String vehicleId,
        @McpToolParam(description = "Tipo: SPEED_LIMIT_EXCEEDED o LOW_FUEL", required = false) String type,
        @McpToolParam(description = "Severidad: INFO, WARNING o CRITICAL", required = false) String severity,
        @McpToolParam(description = "Estado: ACTIVE, ACKNOWLEDGED o RESOLVED", required = false) String status,
        @McpToolParam(description = "Número de página desde cero, máximo 1000", required = false) Integer page,
        @McpToolParam(description = "Tamaño de página entre 1 y 50; predeterminado 20", required = false) Integer size) {
        Map<String, Object> query = pageQuery(page, size);
        if (tripId != null) query.put("tripId", validId(tripId));
        if (vehicleId != null) query.put("vehicleId", validId(vehicleId));
        addEnum(query, "type", type, ALERT_TYPES);
        addEnum(query, "severity", severity, SEVERITIES);
        addEnum(query, "status", status, ALERT_STATUSES);
        return api.alertaObject("/api/v1/alerts", query);
    }

    @McpTool(name = "get_alert", description = "Obtiene una alerta del microservicio ms-alerta por su ID numérico.",
        generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> getAlert(@McpToolParam(description = "ID numérico de la alerta") Long alertId) {
        if (alertId == null || alertId < 1) throw new IllegalArgumentException("alertId debe ser positivo");
        return api.alertaObject("/api/v1/alerts/" + alertId, Map.of());
    }

    @McpTool(name = "list_trip_alerts", description = "Consulta la página de alertas de un viaje desde ms-alerta.",
        generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> listTripAlerts(
        @McpToolParam(description = "ID del viaje") String tripId,
        @McpToolParam(description = "Número de página desde cero, máximo 1000", required = false) Integer page,
        @McpToolParam(description = "Tamaño de página entre 1 y 50; predeterminado 20", required = false) Integer size) {
        return api.alertaObject("/api/v1/trips/" + validId(tripId) + "/alerts", pageQuery(page, size));
    }

    private static Map<String, Object> pageQuery(Integer page, Integer size) {
        int actualPage = page == null ? 0 : page;
        if (actualPage < 0 || actualPage > 1000) throw new IllegalArgumentException("page debe estar entre 0 y 1000");
        return new HashMap<>(Map.of("page", actualPage, "size", bounded(size, 20, 50, "size")));
    }

    private static int bounded(Integer value, int defaultValue, int maximum, String name) {
        int result = value == null ? defaultValue : value;
        if (result < 1 || result > maximum) throw new IllegalArgumentException(name + " debe estar entre 1 y " + maximum);
        return result;
    }

    private static String validId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,80}")) {
            throw new IllegalArgumentException("ID inválido");
        }
        return id;
    }

    private static void addEnum(Map<String, Object> query, String name, String value, Set<String> allowed) {
        if (value == null) return;
        if (!allowed.contains(value)) throw new IllegalArgumentException(name + " inválido");
        query.put(name, value);
    }
}
