package com.fleetops.flota.messaging.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetops.flota.messaging.config.RabbitTopologyProperties;
import com.fleetops.flota.messaging.dto.AlertCreatedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class DomainEventPublisherTests {
    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishesOnlyAfterCommit() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitTopologyProperties properties = mock(RabbitTopologyProperties.class);
        RabbitTopologyProperties.Exchanges exchanges = mock(RabbitTopologyProperties.Exchanges.class);
        when(properties.exchanges()).thenReturn(exchanges);
        when(exchanges.topic()).thenReturn("fleetops.events.topic");
        DomainEventPublisher publisher = new DomainEventPublisher(template, properties);
        TransactionSynchronizationManager.initSynchronization();
        AlertCreatedEvent event = event();
        publisher.publishAlertAfterCommit("fleetops.alert.speed.created", event);
        verify(template, never()).convertAndSend(any(), any(), any(), any(MessagePostProcessor.class), any(CorrelationData.class));
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CorrelationData> correlation = ArgumentCaptor.forClass(CorrelationData.class);
        verify(template).convertAndSend(eq("fleetops.events.topic"), eq("fleetops.alert.speed.created"),
            any(AlertCreatedEvent.class), processor.capture(), correlation.capture());
        assertEquals(event.eventId().toString(), correlation.getValue().getId());
        Message message = processor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
        assertEquals("application/json", message.getMessageProperties().getContentType());
    }

    @Test
    void rollbackDoesNotPublish() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitTopologyProperties properties = mock(RabbitTopologyProperties.class);
        DomainEventPublisher publisher = new DomainEventPublisher(template, properties);
        TransactionSynchronizationManager.initSynchronization();
        publisher.publishAlertAfterCommit("fleetops.alert.fuel.created", event());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(1));
        verify(template, never()).convertAndSend(any(), any(), any(), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    private static AlertCreatedEvent event() {
        return new AlertCreatedEvent(UUID.randomUUID(), "alert.created", 1, Instant.now(),
            "servicio-flota", "correlation", 1L, "trip-1", "vehicle-1",
            "LOW_FUEL", "CRITICAL", "Combustible bajo", "Bajo umbral",
            BigDecimal.TEN, new BigDecimal("15"), Map.of());
    }
}
