package com.fleetops.flota.messaging.consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

class LogConsumerTests {

    @Test
    void listenersUseCentralizedQueueProperties()
        throws NoSuchMethodException {

        assertQueue(
            "receiveAllLogs",
            "${fleetops.messaging.queues.all}"
        );

        assertQueue(
            "receiveErrorLogs",
            "${fleetops.messaging.queues.errors}"
        );

        assertQueue(
            "receiveSecurityLogs",
            "${fleetops.messaging.queues.security}"
        );
    }

    private void assertQueue(
        String methodName,
        String expectedQueue
    ) throws NoSuchMethodException {

        Method method = LogConsumer.class.getDeclaredMethod(
            methodName,
            String.class
        );

        RabbitListener listener =
            method.getAnnotation(RabbitListener.class);

        assertArrayEquals(
            new String[]{expectedQueue},
            listener.queues()
        );
    }
}
