package br.com.fiapx.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobRepository;
import br.com.fiapx.api.domain.VideoJobStatus;
import br.com.fiapx.api.support.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

class VideoStatusListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private VideoJobRepository videoJobRepository;

    private UUID jobId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        videoJobRepository.save(new VideoJob(jobId, userId, "clip.mp4", VideoJobStatus.QUEUED, Instant.now()));
    }

    @Test
    void shouldMarkProcessingWhenProcessingEventArrives() {
        rabbitTemplate.convertAndSend("fiapx.events", "video.processing", new VideoProcessingEvent(jobId));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            VideoJob job = videoJobRepository.findById(jobId).orElseThrow();
            assertThat(job.getStatus()).isEqualTo(VideoJobStatus.PROCESSING);
        });
    }

    @Test
    void shouldMarkCompletedWhenCompletedEventArrives() {
        String outputPath = "/tmp/fiapx-it/output/" + jobId + ".zip";
        rabbitTemplate.convertAndSend("fiapx.events", "video.completed", new VideoCompletedEvent(jobId, outputPath));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            VideoJob job = videoJobRepository.findById(jobId).orElseThrow();
            assertThat(job.getStatus()).isEqualTo(VideoJobStatus.COMPLETED);
            assertThat(job.getOutputPath()).isEqualTo(outputPath);
        });
    }

    @Test
    void shouldMarkFailedAndNotifyWhenFailedEventArrives() {
        rabbitTemplate.convertAndSend("fiapx.events", "video.failed", new VideoFailedEvent(jobId, "ffmpeg indisponível"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            VideoJob job = videoJobRepository.findById(jobId).orElseThrow();
            assertThat(job.getStatus()).isEqualTo(VideoJobStatus.FAILED);
            assertThat(job.getErrorMessage()).isEqualTo("ffmpeg indisponível");
        });
    }
}
