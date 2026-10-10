package com.fleetops.flota.messaging.api;
import com.fleetops.flota.messaging.dto.LogMessage;
import com.fleetops.flota.messaging.model.LogLevel;
import com.fleetops.flota.messaging.producer.LogPublisher;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogProducerController {

    private final LogPublisher logPublisher;

    public LogProducerController(LogPublisher logPublisher) {
        this.logPublisher = logPublisher;
    }

    @PostMapping
    public ResponseEntity<String> sendLog(
        @Valid @RequestBody LogMessage logMessage) {

        try {
            LogLevel level = LogLevel.from(logMessage.level());

            logPublisher.publish(
                level,
                logMessage.message().trim()
            );

            return ResponseEntity.ok(
                "Log enviado con nivel: " + level.name()
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(exception.getMessage());
        }
    }
}
