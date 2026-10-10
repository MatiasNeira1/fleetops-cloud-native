package com.fleetops.flota.messaging.model;

import java.util.Locale;

public enum LogLevel {

    INFO,
    WARNING,
    ERROR,
    SECURITY;

    public static LogLevel from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                "Nivel invalido. Use INFO, WARNING, ERROR o SECURITY"
            );
        }
    }
}
