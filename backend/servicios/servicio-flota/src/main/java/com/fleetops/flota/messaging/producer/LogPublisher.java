package com.fleetops.flota.messaging.producer;

import com.fleetops.flota.messaging.config.RabbitMQConfig;
import com.fleetops.flota.messaging.model.LogLevel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogPublisher {

    private final RabbitTemplate rabbitTemplate;

    public LogPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(LogLevel level, String message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            level.name(),
            message
        );
    }
}
