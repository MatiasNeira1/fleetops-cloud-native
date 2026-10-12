package com.fleetops.flota;

import com.fleetops.flota.application.service.TelemetryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TelemetryProperties.class)
public class ServicioFlotaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicioFlotaApplication.class, args);
	}

}
