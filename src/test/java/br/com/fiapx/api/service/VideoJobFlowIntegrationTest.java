package br.com.fiapx.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.fiapx.api.messaging.VideoRequestedEvent;
import br.com.fiapx.api.support.AbstractIntegrationTest;
import br.com.fiapx.api.support.IntegrationTestRabbitConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class VideoJobFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VideoJobService videoJobService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void createJobShouldPersistAndPublishVideoRequested() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "demo.mp4",
            "video/mp4",
            "conteudo-de-teste".getBytes(StandardCharsets.UTF_8)
        );

        var job = videoJobService.createJob(userId, file);

        assertThat(job.getStatus()).isEqualTo(br.com.fiapx.api.domain.VideoJobStatus.QUEUED);
        assertThat(job.getStoragePath()).isNotBlank();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Message message = rabbitTemplate.receive(IntegrationTestRabbitConfig.REQUESTED_SPY_QUEUE, 500);
            assertThat(message).isNotNull();
            Object payload = rabbitTemplate.getMessageConverter().fromMessage(message);
            assertThat(payload).isInstanceOf(VideoRequestedEvent.class);
            VideoRequestedEvent event = (VideoRequestedEvent) payload;
            assertThat(event.jobId()).isEqualTo(job.getId());
            assertThat(event.userId()).isEqualTo(userId);
            assertThat(event.storagePath()).isEqualTo(job.getStoragePath());
        });
    }
}
