package com.fleetops.alerta;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "FleetOps - Servicio de alertas", version = "v1",
    description = "Consulta paginada y gestión de alertas consumidas desde RabbitMQ."))
public class MsAlertaApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAlertaApplication.class, args);
    }
}
