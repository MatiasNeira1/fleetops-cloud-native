package com.fleetops.flota.messaging.consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fleetops.flota.application.service.TelemetryService;
import com.fleetops.flota.messaging.dto.TelemetryEventPayload;
import com.fleetops.flota.messaging.dto.TelemetryReceivedEvent;
import com.rabbitmq.client.Channel;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class TelemetryConsumerTests {

    @Test
    void acknowledgesOnlyAfterSuccessfulProcessing() throws Exception {
        TelemetryService service = mock(TelemetryService.class);
        TelemetryConsumer consumer = new TelemetryConsumer(service);
        Channel channel = mock(Channel.class);
        Message message = message(19L);

        consumer.consume(event(), message, channel);

        verify(service).process(event().payload(), event().correlationId());
        verify(channel).basicAck(19L, false);
    }

    @Test
    void rejectsFailedMessageWithoutRequeue() throws Exception {
        TelemetryService service = mock(TelemetryService.class);
        doThrow(new IllegalStateException("invalid telemetry"))
            .when(service).process(event().payload(), event().correlationId());
        TelemetryConsumer consumer = new TelemetryConsumer(service);
        Channel channel = mock(Channel.class);

        consumer.consume(event(), message(20L), channel);

        verify(channel).basicNack(20L, false, false);
    }

    private static Message message(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }

    private static TelemetryReceivedEvent event() {
        TelemetryEventPayload payload = new TelemetryEventPayload(
            "event-1", "vehicle-1", null, new BigDecimal("-33.4"),
            new BigDecimal("-70.6"), BigDecimal.TEN, new BigDecimal("50"),
            new BigDecimal("100"), Instant.parse("2026-10-11T12:00:00Z"));
        return new TelemetryReceivedEvent("event-1", "telemetry.received", 1,
            "vehicle-1", "correlation-1", Instant.parse("2026-10-11T12:00:00Z"), payload);
    }
}
