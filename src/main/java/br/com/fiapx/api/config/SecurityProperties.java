package br.com.fiapx.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String defaultUser, String defaultPassword) {
}
