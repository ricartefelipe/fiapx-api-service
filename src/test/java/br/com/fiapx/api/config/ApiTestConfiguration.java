package br.com.fiapx.api.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
@EnableConfigurationProperties({RabbitMqProperties.class, StorageProperties.class})
public class ApiTestConfiguration {

    @Bean
    ApplicationRunner seedDemoUser(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return args -> {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                "fiapx"
            );
            if (count != null && count == 0) {
                jdbcTemplate.update(
                    """
                    INSERT INTO users (id, username, password_hash, email, created_at)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    "00000000-0000-0000-0000-000000000001",
                    "fiapx",
                    passwordEncoder.encode("fiapx123"),
                    "fiapx@fiapx.local"
                );
            }
        };
    }

    @Bean
    br.com.fiapx.api.messaging.VideoEventPublisher videoEventPublisher() {
        return mock(br.com.fiapx.api.messaging.VideoEventPublisher.class);
    }
}
