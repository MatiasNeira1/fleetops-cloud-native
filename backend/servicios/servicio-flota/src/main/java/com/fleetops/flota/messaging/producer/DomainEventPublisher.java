package com.fleetops.flota.messaging.producer;

import com.fleetops.flota.messaging.config.RabbitTopologyProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties properties;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate, RabbitTopologyProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(
            properties.exchanges().topic(),
            routingKey,
            event
        );
    }
}
