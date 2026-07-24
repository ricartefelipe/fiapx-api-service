package br.com.fiapx.api.web;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobStatus;
import br.com.fiapx.api.security.SecurityUtils;
import br.com.fiapx.api.service.VideoJobService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/videos")
public class VideoJobController {

    private final VideoJobService videoJobService;

    public VideoJobController(VideoJobService videoJobService) {
        this.videoJobService = videoJobService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoJobResponse> upload(@RequestPart("file") MultipartFile file) {
        VideoJob job = videoJobService.createJob(SecurityUtils.currentUserId(), file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(VideoJobResponse.from(job));
    }

    @GetMapping
    public ResponseEntity<List<VideoJobResponse>> listJobs() {
        List<VideoJobResponse> jobs = videoJobService.listJobs(SecurityUtils.currentUserId()).stream()
            .map(VideoJobResponse::from)
            .toList();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoJobResponse> getJob(@PathVariable UUID id) {
        VideoJob job = videoJobService.getJob(SecurityUtils.currentUserId(), id);
        return ResponseEntity.ok(VideoJobResponse.from(job));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        Path outputPath = videoJobService.getDownloadPath(SecurityUtils.currentUserId(), id);
        Resource resource = new FileSystemResource(outputPath);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".zip\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    public record VideoJobResponse(
        UUID id,
        String originalFilename,
        VideoJobStatus status,
        Instant createdAt,
        Instant updatedAt,
        String errorMessage,
        String downloadUrl
    ) {
        static VideoJobResponse from(VideoJob job) {
            String downloadUrl = job.getStatus() == VideoJobStatus.COMPLETED
                ? "/api/videos/" + job.getId() + "/download"
                : null;
            return new VideoJobResponse(
                job.getId(),
                job.getOriginalFilename(),
                job.getStatus(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getErrorMessage(),
                downloadUrl
            );
        }
    }
}
