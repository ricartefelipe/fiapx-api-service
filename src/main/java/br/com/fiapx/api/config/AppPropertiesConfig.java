package br.com.fiapx.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RabbitMqProperties.class, StorageProperties.class})
public class AppPropertiesConfig {
}
