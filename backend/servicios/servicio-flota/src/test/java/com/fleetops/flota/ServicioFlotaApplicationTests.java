package com.fleetops.flota;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ServicioFlotaApplicationTests {

	@Test
	void applicationCanBeCreatedWithoutExternalServices() {
		assertDoesNotThrow(ServicioFlotaApplication::new);
	}

}
