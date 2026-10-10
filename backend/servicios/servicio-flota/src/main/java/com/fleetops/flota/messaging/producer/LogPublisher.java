package com.fleetops.flota.messaging.producer;

import com.fleetops.flota.messaging.config.RabbitTopologyProperties;
import com.fleetops.flota.messaging.model.LogLevel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties properties;

    public LogPublisher(
        RabbitTemplate rabbitTemplate,
        RabbitTopologyProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(LogLevel level, String message) {
        rabbitTemplate.convertAndSend(
            properties.exchanges().direct(),
            resolveDirectRoutingKey(level),
            message
        );
    }

    public void publishTopic(String routingKey, String message) {
        rabbitTemplate.convertAndSend(
            properties.exchanges().topic(),
            routingKey,
            message
        );
    }

    private String resolveDirectRoutingKey(LogLevel level) {
        return switch (level) {
            case INFO -> properties.routing().direct().info();
            case WARNING -> properties.routing().direct().warning();
            case ERROR -> properties.routing().direct().error();
            case SECURITY -> properties.routing().direct().security();
        };
    }
}
