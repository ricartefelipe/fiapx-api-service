package br.com.fiapx.api.messaging;

import br.com.fiapx.api.config.RabbitMqProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class VideoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;

    public VideoEventPublisher(RabbitTemplate rabbitTemplate, RabbitMqProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publishVideoRequested(VideoRequestedEvent event) {
        rabbitTemplate.convertAndSend(
            properties.exchange(),
            properties.routingKeyVideoRequested(),
            event
        );
    }
}
