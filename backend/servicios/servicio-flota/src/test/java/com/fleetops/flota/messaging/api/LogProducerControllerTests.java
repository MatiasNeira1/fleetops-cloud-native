package com.fleetops.flota.messaging.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fleetops.flota.messaging.dto.LogMessage;
import com.fleetops.flota.messaging.model.LogLevel;
import com.fleetops.flota.messaging.producer.LogPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LogProducerControllerTests {

    @Test
    void publishesLogWithResolvedLevelAndSimulateFailureFlag() {
        LogPublisher publisher = mock(LogPublisher.class);
        LogProducerController controller =
            new LogProducerController(publisher);

        var response = controller.sendLog(
            new LogMessage(
                "error",
                " Fallo de base de datos ",
                true
            )
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            "Log enviado con nivel: ERROR",
            response.getBody()
        );

        verify(publisher).publish(
            LogLevel.ERROR,
            "Fallo de base de datos",
            true
        );
    }

    @Test
    void publishesLogWithSimulateFailureFalse() {
        LogPublisher publisher = mock(LogPublisher.class);
        LogProducerController controller =
            new LogProducerController(publisher);

        var response = controller.sendLog(
            new LogMessage(
                "INFO",
                "Vehiculo conectado",
                false
            )
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(publisher).publish(
            LogLevel.INFO,
            "Vehiculo conectado",
            false
        );
    }

    @Test
    void rejectsInvalidLogLevel() {
        LogPublisher publisher = mock(LogPublisher.class);
        LogProducerController controller =
            new LogProducerController(publisher);

        var response = controller.sendLog(
            new LogMessage(
                "TRACE",
                "Mensaje no soportado",
                false
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
            "Nivel invalido. Use INFO, WARNING, ERROR o SECURITY",
            response.getBody()
        );
        verifyNoInteractions(publisher);
    }
}
