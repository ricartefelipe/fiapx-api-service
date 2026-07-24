package br.com.fiapx.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(String exchange, String routingKeyVideoRequested) {
}
