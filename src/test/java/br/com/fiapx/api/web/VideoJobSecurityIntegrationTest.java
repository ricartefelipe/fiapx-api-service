package br.com.fiapx.api.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobRepository;
import br.com.fiapx.api.domain.VideoJobStatus;
import br.com.fiapx.api.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class VideoJobSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private VideoJobRepository videoJobRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void uploadShouldReturnUnauthorizedWithoutCredentials() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", "video".getBytes());

        mockMvc.perform(multipart("/videos").file(file))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getJobShouldReturnNotFoundForForeignUser() throws Exception {
        UUID foreignUserId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO users (id, username, password_hash, email, created_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            """,
            foreignUserId,
            "outro-" + foreignUserId.toString().substring(0, 8),
            "hash",
            "outro@fiapx.local"
        );
        UUID jobId = UUID.randomUUID();
        videoJobRepository.save(new VideoJob(jobId, foreignUserId, "clip.mp4", VideoJobStatus.QUEUED, Instant.now()));

        mockMvc.perform(get("/videos/{id}", jobId).with(httpBasic("fiapx", "fiapx123")))
            .andExpect(status().isNotFound());
    }

    @Test
    void uploadShouldReturnBadRequestForEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[0]);

        mockMvc.perform(multipart("/videos").file(file).with(httpBasic("fiapx", "fiapx123")))
            .andExpect(status().isBadRequest());
    }
}
