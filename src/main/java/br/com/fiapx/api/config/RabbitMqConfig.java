package br.com.fiapx.api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
@Profile("!test")
public class RabbitMqConfig {

    @Bean
    TopicExchange fiapxEventsExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    Queue videoProcessingQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.queueProcessing()).build();
    }

    @Bean
    Queue videoCompletedQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.queueCompleted()).build();
    }

    @Bean
    Queue videoFailedQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.queueFailed()).build();
    }

    @Bean
    Binding videoProcessingBinding(Queue videoProcessingQueue, TopicExchange fiapxEventsExchange, RabbitMqProperties properties) {
        return BindingBuilder
            .bind(videoProcessingQueue)
            .to(fiapxEventsExchange)
            .with(properties.routingKeyVideoProcessing());
    }

    @Bean
    Binding videoCompletedBinding(Queue videoCompletedQueue, TopicExchange fiapxEventsExchange, RabbitMqProperties properties) {
        return BindingBuilder
            .bind(videoCompletedQueue)
            .to(fiapxEventsExchange)
            .with(properties.routingKeyVideoCompleted());
    }

    @Bean
    Binding videoFailedBinding(Queue videoFailedQueue, TopicExchange fiapxEventsExchange, RabbitMqProperties properties) {
        return BindingBuilder
            .bind(videoFailedQueue)
            .to(fiapxEventsExchange)
            .with(properties.routingKeyVideoFailed());
    }
}
