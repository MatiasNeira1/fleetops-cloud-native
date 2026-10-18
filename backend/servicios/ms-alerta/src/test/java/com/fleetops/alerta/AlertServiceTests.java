package com.fleetops.alerta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetops.alerta.application.service.AlertNotFoundException;
import com.fleetops.alerta.application.service.AlertService;
import com.fleetops.alerta.domain.model.Alert;
import com.fleetops.alerta.domain.model.AlertSeverity;
import com.fleetops.alerta.domain.model.AlertStatus;
import com.fleetops.alerta.domain.model.AlertType;
import com.fleetops.alerta.infrastructure.messaging.AlertCreatedEvent;
import com.fleetops.alerta.infrastructure.persistence.AlertRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

class AlertServiceTests {
    private final AlertRepository repository = mock(AlertRepository.class);
    private final AlertService service = new AlertService(repository);

    @Test
    void createsAlertOnlyOncePerEventId() {
        AlertCreatedEvent event = event();
        when(repository.existsBySourceEventId(event.eventId())).thenReturn(false, true);
        assertTrue(service.create(event));
        assertFalse(service.create(event));
        verify(repository).saveAndFlush(any(Alert.class));
    }

    @Test
    void rejectsUnsupportedVersion() {
        AlertCreatedEvent event = event();
        AlertCreatedEvent unsupported = new AlertCreatedEvent(event.eventId(), event.eventType(), 2,
            event.occurredAt(), event.source(), event.correlationId(), event.alertId(), event.tripId(),
            event.vehicleId(), event.alertType(), event.severity(), event.title(), event.message(),
            event.measuredValue(), event.thresholdValue(), event.metadata());
        assertThrows(IllegalArgumentException.class, () -> service.create(unsupported));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void filtersAndPaginatesAlerts() {
        var page = PageRequest.of(1, 5);
        when(repository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(java.util.List.of(), page, 0));
        assertEquals(1, service.list("trip-1", "vehicle-1", AlertType.LOW_FUEL,
            AlertSeverity.CRITICAL, AlertStatus.ACTIVE, page).getNumber());
        verify(repository).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void transitionsFromActiveToAcknowledgedThenResolved() {
        Alert alert = alert();
        when(repository.findById(1L)).thenReturn(Optional.of(alert));
        assertEquals(AlertStatus.ACKNOWLEDGED, service.updateStatus(1L, AlertStatus.ACKNOWLEDGED).status());
        assertEquals(AlertStatus.RESOLVED, service.updateStatus(1L, AlertStatus.RESOLVED).status());
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(1L, AlertStatus.ACKNOWLEDGED));
    }

    @Test
    void missingAlertReturnsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(AlertNotFoundException.class, () -> service.get(999L));
    }

    private static Alert alert() {
        return new Alert(UUID.randomUUID(), 1L, "correlation-1", "trip-1", "vehicle-1", AlertType.LOW_FUEL,
            AlertSeverity.CRITICAL, "Combustible bajo", "Bajo umbral", BigDecimal.TEN,
            new BigDecimal("15"), Instant.now());
    }

    private static AlertCreatedEvent event() {
        return new AlertCreatedEvent(UUID.randomUUID(), "alert.created", 1, Instant.now(),
            "servicio-flota", "correlation-1", 1L, "trip-1", "vehicle-1",
            "LOW_FUEL", "CRITICAL", "Combustible bajo", "Bajo umbral",
            BigDecimal.TEN, new BigDecimal("15"), Map.of());
    }
}
