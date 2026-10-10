package com.fleetops.flota.messaging.consumer;


import com.fleetops.flota.messaging.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    @RabbitListener(queues = RabbitMQConfig.ALL_LOGS_QUEUE)
    public void receiveAllLogs(String message) {
        System.out.println("[MONITOR GENERAL] " + message);
    }

    @RabbitListener(queues = RabbitMQConfig.ERRORS_ONLY_QUEUE)
    public void receiveErrorLogs(String message) {
        System.out.println("[ALERTA CRITICA] " + message);
    }

    @RabbitListener(queues = RabbitMQConfig.SECURITY_LOGS_QUEUE)
    public void receiveSecurityLogs(String message) {
        System.out.println("[SEGURIDAD] " + message);
    }
}
