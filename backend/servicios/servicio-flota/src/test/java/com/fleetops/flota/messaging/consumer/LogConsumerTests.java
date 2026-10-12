package com.fleetops.flota.messaging.consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

class LogConsumerTests {

    @Test
    void listenersUseCentralizedQueueProperties()
        throws NoSuchMethodException {

        assertListener(
            "receiveAllLogs",
            "${fleetops.messaging.queues.all}"
        );

        assertListener(
            "receiveErrorLogs",
            "${fleetops.messaging.queues.errors}"
        );

        assertListener(
            "receiveSecurityLogs",
            "${fleetops.messaging.queues.security}"
        );
    }

    @Test
    void allLogsConsumerAcksSuccessfulMessage() throws IOException {
        assertAcksSuccessfulMessage(
            (consumer, channel, deliveryTag, simulateFailure) ->
                consumer.receiveAllLogs(
                    "Mensaje general",
                    channel,
                    deliveryTag,
                    simulateFailure
                )
        );
    }

    @Test
    void allLogsConsumerAcceptsStructuredDomainEvents() throws IOException {
        LogConsumer consumer = new LogConsumer();
        Channel channel = mock(Channel.class);

        consumer.receiveAllLogs(Map.of("eventType", "telemetry.received"), channel, 44L, false);

        verify(channel).basicAck(44L, false);
    }

    @Test
    void errorLogsConsumerAcksSuccessfulMessage() throws IOException {
        assertAcksSuccessfulMessage(
            (consumer, channel, deliveryTag, simulateFailure) ->
                consumer.receiveErrorLogs(
                    "Mensaje de error",
                    channel,
                    deliveryTag,
                    simulateFailure
                )
        );
    }

    @Test
    void securityLogsConsumerAcksSuccessfulMessage() throws IOException {
        assertAcksSuccessfulMessage(
            (consumer, channel, deliveryTag, simulateFailure) ->
                consumer.receiveSecurityLogs(
                    "Mensaje de seguridad",
                    channel,
                    deliveryTag,
                    simulateFailure
                )
        );
    }

    @Test
    void allLogsConsumerNacksControlledFailure() throws IOException {
        assertNacksControlledFailure(
            (consumer, channel, deliveryTag, simulateFailure) ->
                consumer.receiveAllLogs(
                    "Mensaje general",
                    channel,
                    deliveryTag,
                    simulateFailure
                )
        );
    }

    @Test
    void errorLogsConsumerNacksControlledFailure() throws IOException {
        assertNacksControlledFailure(
            (consumer, channel, deliveryTag, simulateFailure) ->
                consumer.receiveErrorLogs(
                    "Mensaje de error",
                    channel,
                    deliveryTag,
                    simulateFailure
                )
        );
    }

    @Test
    void securityLogsConsumerNacksControlledFailure() throws IOException {
        assertNacksControlledFailure(
            (consumer, channel, deliveryTag, simulateFailure) ->
                consumer.receiveSecurityLogs(
                    "Mensaje de seguridad",
                    channel,
                    deliveryTag,
                    simulateFailure
                )
        );
    }

    private void assertListener(
        String methodName,
        String expectedQueue
    ) throws NoSuchMethodException {

        Method method = LogConsumer.class.getDeclaredMethod(
            methodName,
            Object.class,
            Channel.class,
            long.class,
            Boolean.class
        );

        RabbitListener listener =
            method.getAnnotation(RabbitListener.class);

        assertNotNull(listener);
        assertArrayEquals(
            new String[]{expectedQueue},
            listener.queues()
        );
        assertEquals("MANUAL", listener.ackMode());
    }

    private void assertAcksSuccessfulMessage(
        ListenerInvocation invocation
    ) throws IOException {
        LogConsumer consumer = new LogConsumer();
        Channel channel = mock(Channel.class);
        long deliveryTag = 42L;

        invocation.invoke(
            consumer,
            channel,
            deliveryTag,
            false
        );

        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(
            anyLong(),
            anyBoolean(),
            anyBoolean()
        );
    }

    private void assertNacksControlledFailure(
        ListenerInvocation invocation
    ) throws IOException {
        LogConsumer consumer = new LogConsumer();
        Channel channel = mock(Channel.class);
        long deliveryTag = 43L;

        invocation.invoke(
            consumer,
            channel,
            deliveryTag,
            true
        );

        verify(channel).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(
            anyLong(),
            anyBoolean()
        );
    }

    @FunctionalInterface
    private interface ListenerInvocation {

        void invoke(
            LogConsumer consumer,
            Channel channel,
            long deliveryTag,
            Boolean simulateFailure
        ) throws IOException;
    }
}
