package com.fleetops.alerta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.fleetops.alerta.application.service.AlertService;
import com.fleetops.alerta.infrastructure.messaging.AlertConsumer;
import com.fleetops.alerta.infrastructure.messaging.AlertCreatedEvent;
import com.fleetops.alerta.infrastructure.persistence.AlertRepository;
import com.rabbitmq.client.Channel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class AlertConsumerTests {
    private final ObjectMapper mapper = new JsonMapper();
    private AlertService service;
    private AlertConsumer consumer;
    private Channel channel;

    @BeforeEach
    void setup() {
        service = mock(AlertService.class);
        channel = mock(Channel.class);
        consumer = new AlertConsumer(mapper, service, mock(AlertRepository.class));
    }

    @Test
    void persistsThenAcknowledgesValidEvent() throws Exception {
        when(service.create(any())).thenReturn(true);
        consumer.consume(message(event()), channel);
        verify(service).create(any());
        verify(channel).basicAck(7L, false);
    }

    @Test
    void duplicateIsAcknowledgedWithoutNewAlert() throws Exception {
        when(service.create(any())).thenReturn(false);
        consumer.consume(message(event()), channel);
        verify(service).create(any());
        verify(channel).basicAck(7L, false);
    }

    @Test
    void invalidMessageIsRejectedToDlq() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(7L);
        consumer.consume(new Message("not-json".getBytes(), properties), channel);
        verify(channel).basicNack(7L, false, false);
    }

    @Test
    void unsupportedEventVersionIsRejectedToDlq() throws Exception {
        AlertCreatedEvent valid = event();
        AlertCreatedEvent unsupported = new AlertCreatedEvent(valid.eventId(), valid.eventType(), 2,
            valid.occurredAt(), valid.source(), valid.correlationId(), valid.alertId(), valid.tripId(),
            valid.vehicleId(), valid.alertType(), valid.severity(), valid.title(), valid.message(),
            valid.measuredValue(), valid.thresholdValue(), valid.metadata());
        consumer.consume(message(unsupported), channel);
        verify(channel).basicNack(7L, false, false);
    }

    @Test
    void permanentFailureHasBoundedRetriesAndNoRequeue() throws Exception {
        when(service.create(any())).thenThrow(new IllegalStateException("database unavailable"));
        consumer.consume(message(event()), channel);
        verify(service, times(3)).create(any());
        verify(channel).basicNack(7L, false, false);
    }

    private Message message(AlertCreatedEvent event) throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(7L);
        return new Message(mapper.writeValueAsBytes(event), properties);
    }

    private static AlertCreatedEvent event() {
        return new AlertCreatedEvent(UUID.randomUUID(), "alert.created", 1, Instant.now(),
            "servicio-flota", "correlation-1", 4L, "trip-1", "vehicle-1",
            "LOW_FUEL", "CRITICAL", "Combustible bajo", "Bajo umbral",
            BigDecimal.TEN, new BigDecimal("15"), Map.of());
    }
}
