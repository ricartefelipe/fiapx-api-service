package br.com.fiapx.api.support;

import br.com.fiapx.api.config.RabbitMqProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("integration-test")
public class IntegrationTestRabbitConfig {

    public static final String REQUESTED_SPY_QUEUE = "test.video.requested.spy";

    @Bean
    Queue requestedSpyQueue() {
        return QueueBuilder.durable(REQUESTED_SPY_QUEUE).build();
    }

    @Bean
    Binding requestedSpyBinding(Queue requestedSpyQueue, TopicExchange fiapxEventsExchange, RabbitMqProperties properties) {
        return BindingBuilder
            .bind(requestedSpyQueue)
            .to(fiapxEventsExchange)
            .with(properties.routingKeyVideoRequested());
    }
}
