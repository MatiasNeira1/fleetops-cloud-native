package com.fleetops.alerta.application.service;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(Long id) {
        super("No existe la alerta " + id);
    }
}
