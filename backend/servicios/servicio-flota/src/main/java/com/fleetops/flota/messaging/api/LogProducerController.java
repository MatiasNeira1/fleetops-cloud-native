package com.fleetops.flota.messaging.api;
import com.fleetops.flota.messaging.dto.LogMessage;
import com.fleetops.flota.messaging.model.LogLevel;
import com.fleetops.flota.messaging.producer.LogPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs", description = "Publicación de mensajes de prueba en RabbitMQ")
public class LogProducerController {

    private final LogPublisher logPublisher;

    public LogProducerController(LogPublisher logPublisher) {
        this.logPublisher = logPublisher;
    }

    @PostMapping
    @Operation(summary = "Publicar un log", description = "Acepta nivel y mensaje; simulateFailure permite probar la ruta de fallo del consumidor.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Log publicado"),
        @ApiResponse(responseCode = "400", description = "Nivel o mensaje inválido")})
    public ResponseEntity<String> sendLog(
        @Valid @RequestBody LogMessage logMessage) {

        try {
            LogLevel level = LogLevel.from(logMessage.level());

            logPublisher.publish(
                level,
                logMessage.message().trim(),
                logMessage.simulateFailure()
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
