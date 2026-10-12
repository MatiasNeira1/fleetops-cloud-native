package com.fleetops.flota.messaging.consumer;

import com.fleetops.flota.application.service.TelemetryService;
import com.fleetops.flota.messaging.dto.TelemetryReceivedEvent;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryConsumer {

    private final TelemetryService telemetryService;

    public TelemetryConsumer(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @RabbitListener(queues = "${fleetops.messaging.queues.telemetry}")
    public void consume(
        TelemetryReceivedEvent event,
        Message message,
        Channel channel
    ) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            telemetryService.process(event.payload());
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
