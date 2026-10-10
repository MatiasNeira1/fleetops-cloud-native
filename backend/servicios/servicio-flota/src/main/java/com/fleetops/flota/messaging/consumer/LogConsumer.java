package com.fleetops.flota.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(LogConsumer.class);

    @RabbitListener(queues = "${fleetops.messaging.queues.all}")
    public void receiveAllLogs(String message) {
        LOGGER.info("[MONITOR GENERAL] {}", message);
    }

    @RabbitListener(queues = "${fleetops.messaging.queues.errors}")
    public void receiveErrorLogs(String message) {
        LOGGER.error("[ALERTA CRITICA] {}", message);
    }

    @RabbitListener(queues = "${fleetops.messaging.queues.security}")
    public void receiveSecurityLogs(String message) {
        LOGGER.warn("[SEGURIDAD] {}", message);
    }
}
