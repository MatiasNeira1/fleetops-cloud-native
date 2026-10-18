package com.fleetops.flota.messaging.producer;

import com.fleetops.flota.messaging.config.RabbitTopologyProperties;
import com.fleetops.flota.messaging.dto.AlertCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties properties;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate, RabbitTopologyProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        rabbitTemplate.setConfirmCallback((correlation, ack, cause) ->
            log.info("publish_confirm eventId={} ack={} reason={}",
                correlation == null ? "unknown" : correlation.getId(), ack, cause));
        rabbitTemplate.setReturnsCallback(returned ->
            log.warn("publish_return exchange={} routingKey={} replyCode={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode()));
    }

    public void publish(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(
            properties.exchanges().topic(),
            routingKey,
            event
        );
    }

    public void publishAlertAfterCommit(String routingKey, AlertCreatedEvent event) {
        Runnable send = () -> {
            try {
                rabbitTemplate.convertAndSend(properties.exchanges().topic(), routingKey, event,
                    message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    }, new CorrelationData(event.eventId().toString()));
                log.info("publish_requested eventId={} correlationId={}", event.eventId(), event.correlationId());
            } catch (RuntimeException exception) {
                log.error("publish_failed eventId={} correlationId={}", event.eventId(), event.correlationId(), exception);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
