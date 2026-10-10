package com.fleetops.flota.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "fleetops.messaging")
@Validated
public record RabbitTopologyProperties(
    @Valid Exchanges exchanges,
    @Valid Queues queues,
    @Valid Dlqs dlqs,
    @Valid Routing routing
) {

    public record Exchanges(
        @NotBlank String direct,
        @NotBlank String topic,
        @NotBlank String dlx
    ) {
    }

    public record Queues(
        @NotBlank String all,
        @NotBlank String errors,
        @NotBlank String security
    ) {
    }

    public record Dlqs(
        @NotBlank String all,
        @NotBlank String errors,
        @NotBlank String security
    ) {
    }

    public record Routing(
        @Valid Direct direct,
        @Valid Topic topic,
        @Valid Dead dead
    ) {
    }

    public record Direct(
        @NotBlank String info,
        @NotBlank String warning,
        @NotBlank String error,
        @NotBlank String security
    ) {
    }

    public record Topic(
        @NotBlank String all,
        @NotBlank String errors,
        @NotBlank String security
    ) {
    }

    public record Dead(
        @NotBlank String all,
        @NotBlank String errors,
        @NotBlank String security
    ) {
    }
}
