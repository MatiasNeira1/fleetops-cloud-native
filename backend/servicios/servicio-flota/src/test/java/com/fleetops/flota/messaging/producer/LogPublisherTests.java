package com.fleetops.flota.messaging.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fleetops.flota.messaging.config.FleetOpsMessageHeaders;
import com.fleetops.flota.messaging.config.RabbitTopologyProperties;
import com.fleetops.flota.messaging.model.LogLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class LogPublisherTests {

    private RabbitTemplate rabbitTemplate;
    private LogPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new LogPublisher(rabbitTemplate, createProperties());
    }

    @Test
    void publishesToDirectExchange() {
        publisher.publish(
            LogLevel.ERROR,
            "Fallo de base de datos"
        );

        verify(rabbitTemplate).convertAndSend(
            eq("fleetops.logs.direct"),
            eq("ERROR"),
            eq("Fallo de base de datos"),
            any(MessagePostProcessor.class)
        );
    }

    @Test
    void publishesToTopicExchange() {
        publisher.publishTopic(
            "fleetops.vehicle.error",
            "Vehiculo sin conexion"
        );

        verify(rabbitTemplate).convertAndSend(
            eq("fleetops.events.topic"),
            eq("fleetops.vehicle.error"),
            eq("Vehiculo sin conexion"),
            any(MessagePostProcessor.class)
        );
    }

    @Test
    void addsSimulateFailureHeaderWithTrueValue() throws Exception {
        publisher.publish(
            LogLevel.SECURITY,
            "Acceso sospechoso",
            true
        );

        Message processedMessage = applyCapturedPostProcessor();

        assertEquals(
            true,
            processedMessage.getMessageProperties().getHeader(
                FleetOpsMessageHeaders.SIMULATE_FAILURE
            )
        );
    }

    @Test
    void addsSimulateFailureHeaderWithFalseValue() throws Exception {
        publisher.publishTopic(
            "fleetops.vehicle.info",
            "Vehiculo conectado",
            false
        );

        Message processedMessage = applyCapturedPostProcessor();

        assertEquals(
            false,
            processedMessage.getMessageProperties().getHeader(
                FleetOpsMessageHeaders.SIMULATE_FAILURE
            )
        );
    }

    private Message applyCapturedPostProcessor() throws Exception {
        ArgumentCaptor<MessagePostProcessor> captor =
            ArgumentCaptor.forClass(MessagePostProcessor.class);

        verify(rabbitTemplate).convertAndSend(
            any(String.class),
            any(String.class),
            any(Object.class),
            captor.capture()
        );

        Message message = new Message(
            "payload".getBytes(),
            new MessageProperties()
        );

        return captor.getValue().postProcessMessage(message);
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
