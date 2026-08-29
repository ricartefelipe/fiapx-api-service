package br.com.fiapx.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobRepository;
import br.com.fiapx.api.domain.VideoJobStatus;
import br.com.fiapx.api.support.AbstractIntegrationTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

class EmailNotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private VideoJobRepository videoJobRepository;

    private UUID jobId;

    @BeforeEach
    void setUp() throws Exception {
        GREEN_MAIL.purgeEmailFromAllMailboxes();
        jobId = UUID.randomUUID();
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        videoJobRepository.save(new VideoJob(jobId, userId, "clip.mp4", VideoJobStatus.PROCESSING, Instant.now()));
    }

    @Test
    void shouldSendEmailWhenProcessingFails() throws Exception {
        rabbitTemplate.convertAndSend("fiapx.events", "video.failed", new br.com.fiapx.api.messaging.VideoFailedEvent(jobId, "erro simulado"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            MimeMessage[] messages = GREEN_MAIL.getReceivedMessages();
            assertThat(messages).hasSize(1);
            assertThat(messages[0].getSubject()).contains(jobId.toString());
            assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("fiapx@fiapx.local");
        });
    }
}
