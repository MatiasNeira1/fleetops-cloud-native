package com.fleetops.mcp.api;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FleetOpsApiClient {
    private static final Logger log = LoggerFactory.getLogger(FleetOpsApiClient.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};

    private final RestClient flota;
    private final RestClient alerta;

    public FleetOpsApiClient(@Qualifier("flotaRestClient") RestClient flota,
                             @Qualifier("alertaRestClient") RestClient alerta) {
        this.flota = flota;
        this.alerta = alerta;
    }

    public List<Map<String, Object>> flotaList(String path, Map<String, Object> query) {
        return request(flota, "flota", path, query, LIST_TYPE);
    }

    public Map<String, Object> flotaObject(String path) {
        return request(flota, "flota", path, Map.of(), MAP_TYPE);
    }

    public Map<String, Object> alertaObject(String path, Map<String, Object> query) {
        return request(alerta, "alerta", path, query, MAP_TYPE);
    }

    private <T> T request(RestClient client, String service, String path, Map<String, Object> query,
                          ParameterizedTypeReference<T> type) {
        try {
            T body = client.get()
                .uri(builder -> {
                    builder.path(path);
                    query.forEach((name, value) -> {
                        if (value != null) builder.queryParam(name, value);
                    });
                    return builder.build();
                })
                .retrieve().body(type);
            if (body == null) throw new FleetOpsApiException("La API de " + service + " devolvió una respuesta vacía");
            return body;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            log.warn("FleetOps API request failed: service={} status={}", service, status);
            String message = switch (status) {
                case 400 -> "Parámetros rechazados por la API de " + service;
                case 401 -> "Credencial de servicio no aceptada por la API de " + service;
                case 403 -> "Acceso denegado por la API de " + service;
                case 404 -> "Recurso no encontrado en la API de " + service;
                case 429 -> "La API de " + service + " está limitando solicitudes";
                default -> "La API de " + service + " no está disponible (HTTP " + status + ")";
            };
            throw new FleetOpsApiException(message);
        } catch (ResourceAccessException exception) {
            log.warn("FleetOps API connection failed: service={}", service);
            throw new FleetOpsApiException("No se pudo conectar con la API de " + service);
        } catch (RestClientException exception) {
            log.warn("FleetOps API response failed: service={}", service);
            throw new FleetOpsApiException("Respuesta inválida de la API de " + service);
        }
    }
}
