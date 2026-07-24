package br.com.fiapx.api.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMqProperties.class)
public class RabbitMqConfig {

    @Bean
    TopicExchange fiapxEventsExchange(RabbitMqProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }
}
