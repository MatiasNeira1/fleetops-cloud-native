package com.fleetops.flota.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class IncidentTransitionTests {

    @Test
    void allowsOpenToAcknowledgedAndThenResolved() {
        Incident incident = incident();

        incident.changeStatus(IncidentStatus.ACKNOWLEDGED, null);
        assertEquals(IncidentStatus.ACKNOWLEDGED, incident.getStatus());
        incident.changeStatus(IncidentStatus.RESOLVED, "Revisado");

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals("Revisado", incident.getResolutionNote());
        assertNotNull(incident.getResolvedAt());
    }

    @Test
    void allowsOpenToResolved() {
        Incident incident = incident();

        incident.changeStatus(IncidentStatus.RESOLVED, "Solucionado");

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    @Test
    void rejectsSameStateAndReopenTransitions() {
        Incident incident = incident();
        assertThrows(IllegalStateException.class,
            () -> incident.changeStatus(IncidentStatus.OPEN, null));
        incident.changeStatus(IncidentStatus.ACKNOWLEDGED, null);
        assertThrows(IllegalStateException.class,
            () -> incident.changeStatus(IncidentStatus.ACKNOWLEDGED, null));
        incident.changeStatus(IncidentStatus.RESOLVED, null);
        assertThrows(IllegalStateException.class,
            () -> incident.changeStatus(IncidentStatus.OPEN, null));
    }

    private static Incident incident() {
        return new Incident("trip-1", "vehicle-1", IncidentType.GENERAL,
            "Incidente", "Detalle", AlertSeverity.WARNING, null, null, Instant.now());
    }
}
