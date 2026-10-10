package com.fleetops.flota.messaging.producer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fleetops.flota.messaging.config.RabbitTopologyProperties;
import com.fleetops.flota.messaging.model.LogLevel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class LogPublisherTests {

    @Test
    void publishesToDirectExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitTopologyProperties properties = createProperties();

        LogPublisher publisher =
            new LogPublisher(rabbitTemplate, properties);

        publisher.publish(
            LogLevel.ERROR,
            "Fallo de base de datos"
        );

        verify(rabbitTemplate).convertAndSend(
            "fleetops.logs.direct",
            "ERROR",
            "Fallo de base de datos"
        );
    }

    @Test
    void publishesToTopicExchange() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitTopologyProperties properties = createProperties();

        LogPublisher publisher =
            new LogPublisher(rabbitTemplate, properties);

        publisher.publishTopic(
            "fleetops.vehicle.error",
            "Vehiculo sin conexion"
        );

        verify(rabbitTemplate).convertAndSend(
            "fleetops.events.topic",
            "fleetops.vehicle.error",
            "Vehiculo sin conexion"
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
