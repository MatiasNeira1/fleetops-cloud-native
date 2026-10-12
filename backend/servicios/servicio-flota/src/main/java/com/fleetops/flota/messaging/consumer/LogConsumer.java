package com.fleetops.flota.messaging.consumer;

import com.fleetops.flota.messaging.config.FleetOpsMessageHeaders;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(LogConsumer.class);

    @RabbitListener(
        queues = "${fleetops.messaging.queues.all}",
        ackMode = "MANUAL"
    )
    public void receiveAllLogs(
        Object message,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        @Header(
            value = FleetOpsMessageHeaders.SIMULATE_FAILURE,
            required = false
        ) Boolean simulateFailure
    ) throws IOException {
        try {
            failWhenRequested(simulateFailure);

            LOGGER.info(
                "[ACK][MONITOR GENERAL] {}",
                messageBody(message)
            );

            channel.basicAck(deliveryTag, false);
        }
        catch (RuntimeException exception) {
            rejectMessage(
                channel,
                deliveryTag,
                "MONITOR GENERAL",
                exception
            );
        }
    }

    @RabbitListener(
        queues = "${fleetops.messaging.queues.errors}",
        ackMode = "MANUAL"
    )
    public void receiveErrorLogs(
        Object message,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        @Header(
            value = FleetOpsMessageHeaders.SIMULATE_FAILURE,
            required = false
        ) Boolean simulateFailure
    ) throws IOException {
        try {
            failWhenRequested(simulateFailure);

            LOGGER.error(
                "[ACK][ALERTA CRITICA] {}",
                messageBody(message)
            );

            channel.basicAck(deliveryTag, false);
        }
        catch (RuntimeException exception) {
            rejectMessage(
                channel,
                deliveryTag,
                "ALERTA CRITICA",
                exception
            );
        }
    }

    @RabbitListener(
        queues = "${fleetops.messaging.queues.security}",
        ackMode = "MANUAL"
    )
    public void receiveSecurityLogs(
        Object message,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
        @Header(
            value = FleetOpsMessageHeaders.SIMULATE_FAILURE,
            required = false
        ) Boolean simulateFailure
    ) throws IOException {
        try {
            failWhenRequested(simulateFailure);

            LOGGER.warn(
                "[ACK][SEGURIDAD] {}",
                messageBody(message)
            );

            channel.basicAck(deliveryTag, false);
        }
        catch (RuntimeException exception) {
            rejectMessage(
                channel,
                deliveryTag,
                "SEGURIDAD",
                exception
            );
        }
    }

    private void failWhenRequested(Boolean simulateFailure) {
        if (Boolean.TRUE.equals(simulateFailure)) {
            throw new IllegalStateException(
                "Fallo controlado solicitado"
            );
        }
    }

    private String messageBody(Object message) {
        if (message instanceof Message amqpMessage) {
            return new String(amqpMessage.getBody(), StandardCharsets.UTF_8);
        }
        return String.valueOf(message);
    }

    private void rejectMessage(
        Channel channel,
        long deliveryTag,
        String consumer,
        RuntimeException exception
    ) throws IOException {
        LOGGER.error(
            "[NACK][{}] Mensaje rechazado: {}",
            consumer,
            exception.getMessage()
        );

        channel.basicNack(
            deliveryTag,
            false,
            false
        );
    }
}
