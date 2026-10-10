package com.fleetops.flota.messaging.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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

        assertTrue(allQueue.isDurable());
        assertTrue(errorsQueue.isDurable());
        assertTrue(securityQueue.isDurable());

        assertEquals(
            properties.exchanges().dlx(),
            allQueue.getArguments().get("x-dead-letter-exchange")
        );

        assertEquals(
            properties.routing().dead().all(),
            allQueue.getArguments().get("x-dead-letter-routing-key")
        );

        assertEquals(
            properties.routing().dead().errors(),
            errorsQueue.getArguments().get("x-dead-letter-routing-key")
        );

        assertEquals(
            properties.routing().dead().security(),
            securityQueue.getArguments().get("x-dead-letter-routing-key")
        );
    }

    @Test
    void declaresThreeDurableDeadLetterQueues() {
        assertTrue(config.allLogsDlq().isDurable());
        assertTrue(config.errorsOnlyDlq().isDurable());
        assertTrue(config.securityLogsDlq().isDurable());

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
    }

    @Test
    void declaresDirectAndTopicBindings() {
        var directBinding = config.bindInfo(
            config.logsDirectExchange(),
            config.allLogsQueue()
        );

        var topicBinding = config.bindAllTopicEvents(
            config.eventsTopicExchange(),
            config.allLogsQueue()
        );

        assertEquals(
            properties.routing().direct().info(),
            directBinding.getRoutingKey()
        );

        assertEquals(
            properties.routing().topic().all(),
            topicBinding.getRoutingKey()
        );
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
                "fleetops.logs.security.queue"
            ),
            new RabbitTopologyProperties.Dlqs(
                "fleetops.logs.all.dlq",
                "fleetops.logs.errors.dlq",
                "fleetops.logs.security.dlq"
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
                    "fleetops.security.#"
                ),
                new RabbitTopologyProperties.Dead(
                    "fleetops.logs.all.dead",
                    "fleetops.logs.errors.dead",
                    "fleetops.logs.security.dead"
                )
            )
        );
    }
}
