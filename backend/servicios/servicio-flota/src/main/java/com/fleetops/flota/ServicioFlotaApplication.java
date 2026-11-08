package com.fleetops.flota;

import com.fleetops.flota.application.service.TelemetryProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TelemetryProperties.class)
@OpenAPIDefinition(info = @Info(title = "FleetOps - Servicio de flota", version = "v1",
    description = "Viajes, vehículos, telemetría, incidentes y alertas operativas."))
public class ServicioFlotaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicioFlotaApplication.class, args);
	}

}
