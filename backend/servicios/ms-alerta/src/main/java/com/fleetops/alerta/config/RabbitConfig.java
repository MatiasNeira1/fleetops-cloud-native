package com.fleetops.alerta.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public TopicExchange alertsExchange(@Value("${fleetops.alerts.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public DirectExchange alertsDlx(@Value("${fleetops.alerts.dlx}") String name) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    public Queue alertsQueue(@Value("${fleetops.alerts.queue}") String name,
                      @Value("${fleetops.alerts.dlx}") String dlx,
                      @Value("${fleetops.alerts.dlq-routing-key}") String deadKey) {
        return QueueBuilder.durable(name).deadLetterExchange(dlx)
            .deadLetterRoutingKey(deadKey).build();
    }

    @Bean
    public Queue alertsDlq(@Value("${fleetops.alerts.dlq}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Binding bindAlerts(@Qualifier("alertsQueue") Queue queue,
                       @Qualifier("alertsExchange") TopicExchange exchange,
                       @Value("${fleetops.alerts.routing-pattern}") String pattern) {
        return BindingBuilder.bind(queue).to(exchange).with(pattern);
    }

    @Bean
    public Binding bindAlertsDlq(@Qualifier("alertsDlq") Queue queue,
                          @Qualifier("alertsDlx") DirectExchange exchange,
                          @Value("${fleetops.alerts.dlq-routing-key}") String key) {
        return BindingBuilder.bind(queue).to(exchange).with(key);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
