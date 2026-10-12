package com.fleetops.flota.messaging.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;

class RabbitMQConfigTests {

    private final RabbitTopologyProperties properties = createProperties();
    private final RabbitMQConfig config = new RabbitMQConfig(properties);

    @Test
    void declaresThreeDurableExchanges() {
        assertEquals(
            properties.exchanges().direct(),
            config.logsDirectExchange().getName()
        );

        assertEquals(
            properties.exchanges().topic(),
            config.eventsTopicExchange().getName()
        );

        assertEquals(
            properties.exchanges().dlx(),
            config.deadLetterExchange().getName()
        );

        assertTrue(config.logsDirectExchange().isDurable());
        assertTrue(config.eventsTopicExchange().isDurable());
        assertTrue(config.deadLetterExchange().isDurable());
    }

    @Test
    void declaresMainQueuesWithDeadLetterConfiguration() {
        var allQueue = config.allLogsQueue();
        var errorsQueue = config.errorsOnlyQueue();
        var securityQueue = config.securityLogsQueue();
        var telemetryQueue = config.telemetryQueue();

        assertMainQueue(
            allQueue,
            properties.queues().all(),
            properties.routing().dead().all()
        );

        assertMainQueue(
            errorsQueue,
            properties.queues().errors(),
            properties.routing().dead().errors()
        );

        assertMainQueue(
            securityQueue,
            properties.queues().security(),
            properties.routing().dead().security()
        );

        assertMainQueue(
            telemetryQueue,
            properties.queues().telemetry(),
            properties.routing().dead().telemetry()
        );
    }

    @Test
    void declaresThreeDurableDeadLetterQueues() {
        assertTrue(config.allLogsDlq().isDurable());
        assertTrue(config.errorsOnlyDlq().isDurable());
        assertTrue(config.securityLogsDlq().isDurable());
        assertTrue(config.telemetryDlq().isDurable());

        assertEquals(
            properties.dlqs().all(),
            config.allLogsDlq().getName()
        );

        assertEquals(
            properties.dlqs().errors(),
            config.errorsOnlyDlq().getName()
        );

        assertEquals(
            properties.dlqs().security(),
            config.securityLogsDlq().getName()
        );

        assertEquals(
            properties.dlqs().telemetry(),
            config.telemetryDlq().getName()
        );
    }

    @Test
    void declaresDirectAndTopicBindings() {
        assertBinding(
            config.bindInfo(
                config.logsDirectExchange(),
                config.allLogsQueue()
            ),
            properties.exchanges().direct(),
            properties.queues().all(),
            properties.routing().direct().info()
        );

        assertBinding(
            config.bindWarning(
                config.logsDirectExchange(),
                config.allLogsQueue()
            ),
            properties.exchanges().direct(),
            properties.queues().all(),
            properties.routing().direct().warning()
        );

        assertBinding(
            config.bindErrorGeneral(
                config.logsDirectExchange(),
                config.allLogsQueue()
            ),
            properties.exchanges().direct(),
            properties.queues().all(),
            properties.routing().direct().error()
        );

        assertBinding(
            config.bindErrorCritical(
                config.logsDirectExchange(),
                config.errorsOnlyQueue()
            ),
            properties.exchanges().direct(),
            properties.queues().errors(),
            properties.routing().direct().error()
        );

        assertBinding(
            config.bindSecurity(
                config.logsDirectExchange(),
                config.securityLogsQueue()
            ),
            properties.exchanges().direct(),
            properties.queues().security(),
            properties.routing().direct().security()
        );

        assertBinding(
            config.bindAllTopicEvents(
                config.eventsTopicExchange(),
                config.allLogsQueue()
            ),
            properties.exchanges().topic(),
            properties.queues().all(),
            properties.routing().topic().all()
        );

        assertBinding(
            config.bindErrorTopicEvents(
                config.eventsTopicExchange(),
                config.errorsOnlyQueue()
            ),
            properties.exchanges().topic(),
            properties.queues().errors(),
            properties.routing().topic().errors()
        );

        assertBinding(
            config.bindSecurityTopicEvents(
                config.eventsTopicExchange(),
                config.securityLogsQueue()
            ),
            properties.exchanges().topic(),
            properties.queues().security(),
            properties.routing().topic().security()
        );

        assertBinding(
            config.bindTelemetryTopicEvents(
                config.eventsTopicExchange(),
                config.telemetryQueue()
            ),
            properties.exchanges().topic(),
            properties.queues().telemetry(),
            properties.routing().topic().telemetry()
        );
    }

    @Test
    void declaresDeadLetterQueueBindings() {
        assertBinding(
            config.bindAllLogsDlq(
                config.deadLetterExchange(),
                config.allLogsDlq()
            ),
            properties.exchanges().dlx(),
            properties.dlqs().all(),
            properties.routing().dead().all()
        );

        assertBinding(
            config.bindErrorsDlq(
                config.deadLetterExchange(),
                config.errorsOnlyDlq()
            ),
            properties.exchanges().dlx(),
            properties.dlqs().errors(),
            properties.routing().dead().errors()
        );

        assertBinding(
            config.bindSecurityDlq(
                config.deadLetterExchange(),
                config.securityLogsDlq()
            ),
            properties.exchanges().dlx(),
            properties.dlqs().security(),
            properties.routing().dead().security()
        );

        assertBinding(
            config.bindTelemetryDlq(
                config.deadLetterExchange(),
                config.telemetryDlq()
            ),
            properties.exchanges().dlx(),
            properties.dlqs().telemetry(),
            properties.routing().dead().telemetry()
        );
    }

    private void assertMainQueue(
        Queue queue,
        String expectedName,
        String expectedDeadLetterRoutingKey
    ) {
        assertEquals(expectedName, queue.getName());
        assertTrue(queue.isDurable());

        assertEquals(
            properties.exchanges().dlx(),
            queue.getArguments().get("x-dead-letter-exchange")
        );

        assertEquals(
            expectedDeadLetterRoutingKey,
            queue.getArguments().get("x-dead-letter-routing-key")
        );
    }

    private static void assertBinding(
        Binding binding,
        String expectedExchange,
        String expectedDestination,
        String expectedRoutingKey
    ) {
        assertEquals(expectedExchange, binding.getExchange());
        assertEquals(expectedDestination, binding.getDestination());
        assertEquals(expectedRoutingKey, binding.getRoutingKey());
    }

    private static RabbitTopologyProperties createProperties() {
        return new RabbitTopologyProperties(
            new RabbitTopologyProperties.Exchanges(
                "fleetops.logs.direct",
                "fleetops.events.topic",
                "fleetops.dlx.direct"
            ),
            new RabbitTopologyProperties.Queues(
                "fleetops.logs.all.queue",
                "fleetops.logs.errors.queue",
                "fleetops.logs.security.queue",
                "fleetops.telemetry.queue"
            ),
            new RabbitTopologyProperties.Dlqs(
                "fleetops.logs.all.dlq",
                "fleetops.logs.errors.dlq",
                "fleetops.logs.security.dlq",
                "fleetops.telemetry.dlq"
            ),
            new RabbitTopologyProperties.Routing(
                new RabbitTopologyProperties.Direct(
                    "INFO",
                    "WARNING",
                    "ERROR",
                    "SECURITY"
                ),
                new RabbitTopologyProperties.Topic(
                    "fleetops.#",
                    "fleetops.*.error",
                    "fleetops.security.#",
                    "fleetops.telemetry.received"
                ),
                new RabbitTopologyProperties.Dead(
                    "fleetops.logs.all.dead",
                    "fleetops.logs.errors.dead",
                    "fleetops.logs.security.dead",
                    "fleetops.telemetry.dead"
                )
            )
        );
    }
}
