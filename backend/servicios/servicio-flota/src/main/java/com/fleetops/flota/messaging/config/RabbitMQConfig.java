package com.fleetops.flota.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitTopologyProperties.class)
public class RabbitMQConfig {

    private final RabbitTopologyProperties properties;

    public RabbitMQConfig(RabbitTopologyProperties properties) {
        this.properties = properties;
    }

    // Exchanges

    @Bean
    DirectExchange logsDirectExchange() {
        return new DirectExchange(
            properties.exchanges().direct(),
            true,
            false
        );
    }

    @Bean
    TopicExchange eventsTopicExchange() {
        return new TopicExchange(
            properties.exchanges().topic(),
            true,
            false
        );
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(
            properties.exchanges().dlx(),
            true,
            false
        );
    }

    // Colas principales

    @Bean
    Queue allLogsQueue() {
        return QueueBuilder
            .durable(properties.queues().all())
            .deadLetterExchange(properties.exchanges().dlx())
            .deadLetterRoutingKey(properties.routing().dead().all())
            .build();
    }

    @Bean
    Queue errorsOnlyQueue() {
        return QueueBuilder
            .durable(properties.queues().errors())
            .deadLetterExchange(properties.exchanges().dlx())
            .deadLetterRoutingKey(properties.routing().dead().errors())
            .build();
    }

    @Bean
    Queue securityLogsQueue() {
        return QueueBuilder
            .durable(properties.queues().security())
            .deadLetterExchange(properties.exchanges().dlx())
            .deadLetterRoutingKey(properties.routing().dead().security())
            .build();
    }

    // Dead Letter Queues

    @Bean
    Queue allLogsDlq() {
        return QueueBuilder
            .durable(properties.dlqs().all())
            .build();
    }

    @Bean
    Queue errorsOnlyDlq() {
        return QueueBuilder
            .durable(properties.dlqs().errors())
            .build();
    }

    @Bean
    Queue securityLogsDlq() {
        return QueueBuilder
            .durable(properties.dlqs().security())
            .build();
    }

    // Bindings del Direct Exchange

    @Bean
    Binding bindInfo(
        @Qualifier("logsDirectExchange") DirectExchange exchange,
        @Qualifier("allLogsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().direct().info());
    }

    @Bean
    Binding bindWarning(
        @Qualifier("logsDirectExchange") DirectExchange exchange,
        @Qualifier("allLogsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().direct().warning());
    }

    @Bean
    Binding bindErrorGeneral(
        @Qualifier("logsDirectExchange") DirectExchange exchange,
        @Qualifier("allLogsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().direct().error());
    }

    @Bean
    Binding bindErrorCritical(
        @Qualifier("logsDirectExchange") DirectExchange exchange,
        @Qualifier("errorsOnlyQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().direct().error());
    }

    @Bean
    Binding bindSecurity(
        @Qualifier("logsDirectExchange") DirectExchange exchange,
        @Qualifier("securityLogsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().direct().security());
    }

    // Bindings del Topic Exchange

    @Bean
    Binding bindAllTopicEvents(
        @Qualifier("eventsTopicExchange") TopicExchange exchange,
        @Qualifier("allLogsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().topic().all());
    }

    @Bean
    Binding bindErrorTopicEvents(
        @Qualifier("eventsTopicExchange") TopicExchange exchange,
        @Qualifier("errorsOnlyQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().topic().errors());
    }

    @Bean
    Binding bindSecurityTopicEvents(
        @Qualifier("eventsTopicExchange") TopicExchange exchange,
        @Qualifier("securityLogsQueue") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().topic().security());
    }

    // Bindings de las DLQ

    @Bean
    Binding bindAllLogsDlq(
        @Qualifier("deadLetterExchange") DirectExchange exchange,
        @Qualifier("allLogsDlq") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().dead().all());
    }

    @Bean
    Binding bindErrorsDlq(
        @Qualifier("deadLetterExchange") DirectExchange exchange,
        @Qualifier("errorsOnlyDlq") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().dead().errors());
    }

    @Bean
    Binding bindSecurityDlq(
        @Qualifier("deadLetterExchange") DirectExchange exchange,
        @Qualifier("securityLogsDlq") Queue queue
    ) {
        return BindingBuilder.bind(queue)
            .to(exchange)
            .with(properties.routing().dead().security());
    }
}
