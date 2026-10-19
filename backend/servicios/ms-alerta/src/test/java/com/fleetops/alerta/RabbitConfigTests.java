package com.fleetops.alerta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fleetops.alerta.config.RabbitConfig;
import org.junit.jupiter.api.Test;

class RabbitConfigTests {
    private final RabbitConfig config = new RabbitConfig();

    @Test
    void durableQueueHasCorrectDeadLetterArguments() {
        var queue = config.alertsQueue("fleetops.alerts.queue", "fleetops.dlx.direct", "fleetops.alerts.dlq");
        assertTrue(queue.isDurable());
        assertEquals("fleetops.dlx.direct", queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("fleetops.alerts.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
        assertTrue(config.alertsDlq("fleetops.alerts.dlq").isDurable());
    }

    @Test
    void bindsOnlyBusinessQueueToAlertPattern() {
        var main = config.bindAlerts(config.alertsQueue("fleetops.alerts.queue", "fleetops.dlx.direct", "fleetops.alerts.dlq"),
            config.alertsExchange("fleetops.events.topic"), "fleetops.alert.#");
        var dead = config.bindAlertsDlq(config.alertsDlq("fleetops.alerts.dlq"),
            config.alertsDlx("fleetops.dlx.direct"), "fleetops.alerts.dlq");
        assertEquals("fleetops.alert.#", main.getRoutingKey());
        assertEquals("fleetops.alerts.dlq", dead.getRoutingKey());
    }
}
