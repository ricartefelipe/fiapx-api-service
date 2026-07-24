package br.com.fiapx.api.web;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobRepository;
import br.com.fiapx.api.domain.VideoJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/videos")
public class VideoJobController {

    private final VideoJobRepository videoJobRepository;

    public VideoJobController(VideoJobRepository videoJobRepository) {
        this.videoJobRepository = videoJobRepository;
    }

    @GetMapping
    public ResponseEntity<List<VideoJobResponse>> listJobs() {
        List<VideoJobResponse> jobs = videoJobRepository.findAll().stream()
            .map(VideoJobResponse::from)
            .toList();
        return ResponseEntity.ok(jobs);
    }

    public record VideoJobResponse(
        UUID id,
        String originalFilename,
        VideoJobStatus status,
        Instant createdAt,
        Instant updatedAt
    ) {
        static VideoJobResponse from(VideoJob job) {
            return new VideoJobResponse(
                job.getId(),
                job.getOriginalFilename(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt()
            );
        }
    }
}
