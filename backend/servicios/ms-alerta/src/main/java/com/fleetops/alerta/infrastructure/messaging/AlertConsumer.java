package com.fleetops.alerta.infrastructure.messaging;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import com.fleetops.alerta.application.service.AlertService;
import com.fleetops.alerta.infrastructure.persistence.AlertRepository;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class AlertConsumer {
    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);
    private final ObjectMapper mapper;
    private final AlertService service;
    private final AlertRepository repository;

    public AlertConsumer(ObjectMapper mapper, AlertService service, AlertRepository repository) {
        this.mapper = mapper;
        this.service = service;
        this.repository = repository;
    }

    @RabbitListener(queues = "${fleetops.alerts.queue}")
    public void consume(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        AlertCreatedEvent event;
        try {
            event = mapper.readValue(message.getBody(), AlertCreatedEvent.class);
            if (event == null) throw new IllegalArgumentException("Evento vacío");
            event.validate();
        } catch (JacksonException | IllegalArgumentException exception) {
            log.warn("alert_rejected eventId={} correlationId={} reason={}",
                eventId(message), correlationId(message), exception.getClass().getSimpleName());
            channel.basicNack(tag, false, false);
            return;
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                boolean created = service.create(event);
                log.info("alert_consumed eventId={} correlationId={} result={}",
                    event.eventId(), event.correlationId(), created ? "created" : "duplicate");
                channel.basicAck(tag, false);
                return;
            } catch (DataIntegrityViolationException exception) {
                if (repository.existsBySourceEventId(event.eventId())) {
                    log.info("alert_consumed eventId={} correlationId={} result=duplicate", event.eventId(), event.correlationId());
                    channel.basicAck(tag, false);
                    return;
                }
                if (attempt == 3) reject(channel, tag, event, exception);
            } catch (IllegalArgumentException exception) {
                reject(channel, tag, event, exception);
                return;
            } catch (RuntimeException exception) {
                if (attempt == 3) reject(channel, tag, event, exception);
            }
        }
    }

    private void reject(Channel channel, long tag, AlertCreatedEvent event, Exception exception) throws IOException {
        log.warn("alert_rejected eventId={} correlationId={} reason={}",
            event.eventId(), event.correlationId(), exception.getClass().getSimpleName());
        channel.basicNack(tag, false, false);
    }

    private String eventId(Message message) {
        try { return mapper.readTree(message.getBody()).path("eventId").asText("unknown"); }
        catch (Exception exception) { return "unknown"; }
    }

    private String correlationId(Message message) {
        try { return mapper.readTree(message.getBody()).path("correlationId").asText("unknown"); }
        catch (Exception exception) { return "unknown"; }
    }
}
