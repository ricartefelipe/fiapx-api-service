package br.com.fiapx.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.api.config.StorageProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class VideoStorageServiceTest {

    @TempDir
    Path tempDir;

    private VideoStorageService videoStorageService;

    @BeforeEach
    void setUp() throws Exception {
        videoStorageService = new VideoStorageService(new StorageProperties(tempDir.toString(), tempDir.resolve("output").toString()));
    }

    @Test
    void shouldStoreUploadedFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "video.mp4",
            "video/mp4",
            "conteudo".getBytes(StandardCharsets.UTF_8)
        );

        String storagePath = videoStorageService.store(jobId, file);

        assertThat(Files.exists(Path.of(storagePath))).isTrue();
        assertThat(storagePath).endsWith(".mp4");
    }

    @Test
    void shouldUseDefaultExtensionWhenMissing() throws Exception {
        UUID jobId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "video", "video/mp4", "x".getBytes());

        String storagePath = videoStorageService.store(jobId, file);

        assertThat(storagePath).endsWith(".mp4");
    }

    @Test
    void resolveOutputPathShouldReturnAbsolutePath() {
        Path output = videoStorageService.resolveOutputPath("/tmp/output.zip");
        assertThat(output).isEqualTo(Path.of("/tmp/output.zip"));
    }
}
