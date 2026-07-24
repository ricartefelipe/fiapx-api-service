package br.com.fiapx.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobStatus;
import br.com.fiapx.api.service.VideoJobService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class VideoJobControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private VideoJobService videoJobService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void uploadShouldReturnAccepted() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        VideoJob job = new VideoJob(jobId, userId, "clip.mp4", VideoJobStatus.QUEUED, Instant.now());
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", "video".getBytes());
        when(videoJobService.createJob(any(), any())).thenReturn(job);

        mockMvc.perform(multipart("/videos").file(file).with(httpBasic("fiapx", "fiapx123")))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(jobId.toString()))
            .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void listJobsShouldReturnUserJobs() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        VideoJob job = new VideoJob(jobId, userId, "clip.mp4", VideoJobStatus.COMPLETED, Instant.now());
        when(videoJobService.listJobs(any())).thenReturn(List.of(job));

        mockMvc.perform(get("/videos").with(httpBasic("fiapx", "fiapx123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].downloadUrl").value("/api/videos/" + jobId + "/download"));
    }

    @Test
    void downloadShouldReturnZipWhenReady() throws Exception {
        UUID jobId = UUID.randomUUID();
        Path zipPath = Files.createTempFile("frames", ".zip");
        Files.writeString(zipPath, "zip");
        when(videoJobService.getDownloadPath(any(), eq(jobId))).thenReturn(zipPath);

        mockMvc.perform(get("/videos/{id}/download", jobId).with(httpBasic("fiapx", "fiapx123")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + jobId + ".zip\""));
    }
}
