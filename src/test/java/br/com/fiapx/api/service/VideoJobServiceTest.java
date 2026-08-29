package br.com.fiapx.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobRepository;
import br.com.fiapx.api.domain.VideoJobStatus;
import br.com.fiapx.api.messaging.VideoEventPublisher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VideoJobServiceTest {

    @Mock
    private VideoJobRepository videoJobRepository;

    @Mock
    private VideoStorageService videoStorageService;

    @Mock
    private VideoEventPublisher videoEventPublisher;

    private VideoJobService videoJobService;

    @TempDir
    java.nio.file.Path tempDir;

    @BeforeEach
    void setUp() {
        videoJobService = new VideoJobService(videoJobRepository, videoStorageService, videoEventPublisher);
    }

    @Test
    void createJobShouldPersistAndPublishEvent() throws IOException {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "video.mp4",
            "video/mp4",
            "conteudo".getBytes(StandardCharsets.UTF_8)
        );
        when(videoStorageService.store(any(), any())).thenReturn(tempDir.resolve("video.mp4").toString());
        when(videoJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VideoJob job = videoJobService.createJob(userId, file);

        assertThat(job.getStatus()).isEqualTo(VideoJobStatus.QUEUED);
        verify(videoEventPublisher).publishVideoRequested(any());
    }

    @Test
    void getJobShouldRejectForeignUser() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(videoJobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoJobService.getJob(userId, jobId))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void markProcessingShouldUpdateStatus() {
        UUID jobId = UUID.randomUUID();
        VideoJob job = new VideoJob(jobId, UUID.randomUUID(), "video.mp4", VideoJobStatus.QUEUED, java.time.Instant.now());
        when(videoJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        VideoJob updated = videoJobService.markProcessing(jobId);

        assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.PROCESSING);
    }

    @Test
    void markFailedShouldNotifyUser() {
        UUID jobId = UUID.randomUUID();
        VideoJob job = new VideoJob(jobId, UUID.randomUUID(), "video.mp4", VideoJobStatus.PROCESSING, java.time.Instant.now());
        NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
        when(videoJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        videoJobService.markFailed(jobId, "erro ffmpeg", notificationService);

        assertThat(job.getStatus()).isEqualTo(VideoJobStatus.FAILED);
        verify(notificationService).notifyProcessingFailed(jobId, job.getUserId(), "erro ffmpeg");
    }

    @Test
    void markCompletedShouldUpdateOutputPath() {
        UUID jobId = UUID.randomUUID();
        VideoJob job = new VideoJob(jobId, UUID.randomUUID(), "video.mp4", VideoJobStatus.PROCESSING, java.time.Instant.now());
        when(videoJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        VideoJob updated = videoJobService.markCompleted(jobId, "/tmp/output.zip");

        assertThat(updated.getStatus()).isEqualTo(VideoJobStatus.COMPLETED);
        assertThat(updated.getOutputPath()).isEqualTo("/tmp/output.zip");
    }

    @Test
    void getDownloadPathShouldRejectIncompleteJob() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        VideoJob job = new VideoJob(jobId, userId, "video.mp4", VideoJobStatus.PROCESSING, java.time.Instant.now());
        when(videoJobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> videoJobService.getDownloadPath(userId, jobId))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getDownloadPathShouldReturnExistingZip(@TempDir Path tempDir) throws IOException {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        Path zipPath = tempDir.resolve("output.zip");
        Files.writeString(zipPath, "zip");
        VideoJob job = new VideoJob(jobId, userId, "video.mp4", VideoJobStatus.COMPLETED, java.time.Instant.now());
        job.markCompleted(zipPath.toString(), java.time.Instant.now());
        when(videoJobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.of(job));
        when(videoStorageService.resolveOutputPath(zipPath.toString())).thenReturn(zipPath);

        Path result = videoJobService.getDownloadPath(userId, jobId);

        assertThat(result).isEqualTo(zipPath);
    }

    @Test
    void createJobShouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[0]);

        assertThatThrownBy(() -> videoJobService.createJob(UUID.randomUUID(), file))
            .isInstanceOf(ResponseStatusException.class);
    }
}
