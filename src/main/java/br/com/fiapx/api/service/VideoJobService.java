package br.com.fiapx.api.service;

import br.com.fiapx.api.domain.VideoJob;
import br.com.fiapx.api.domain.VideoJobRepository;
import br.com.fiapx.api.domain.VideoJobStatus;
import br.com.fiapx.api.messaging.VideoEventPublisher;
import br.com.fiapx.api.messaging.VideoRequestedEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VideoJobService {

    private final VideoJobRepository videoJobRepository;
    private final VideoStorageService videoStorageService;
    private final VideoEventPublisher videoEventPublisher;

    public VideoJobService(
        VideoJobRepository videoJobRepository,
        VideoStorageService videoStorageService,
        VideoEventPublisher videoEventPublisher
    ) {
        this.videoJobRepository = videoJobRepository;
        this.videoStorageService = videoStorageService;
        this.videoEventPublisher = videoEventPublisher;
    }

    @Transactional
    @CacheEvict(cacheNames = "videoJobs", key = "#userId")
    public VideoJob createJob(UUID userId, MultipartFile file) {
        validateFile(file);
        UUID jobId = UUID.randomUUID();
        Instant now = Instant.now();
        VideoJob job = new VideoJob(jobId, userId, file.getOriginalFilename(), VideoJobStatus.PENDING, now);
        try {
            String storagePath = videoStorageService.store(jobId, file);
            job.markQueued(storagePath, Instant.now());
            videoJobRepository.save(job);
            videoEventPublisher.publishVideoRequested(new VideoRequestedEvent(
                job.getId(),
                userId,
                job.getOriginalFilename(),
                storagePath
            ));
            return job;
        } catch (IOException exception) {
            job.markFailed("Falha ao salvar arquivo", Instant.now());
            videoJobRepository.save(job);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao salvar arquivo", exception);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "videoJobs", key = "#userId")
    public List<VideoJob> listJobs(UUID userId) {
        return videoJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public VideoJob getJob(UUID userId, UUID jobId) {
        return videoJobRepository.findByIdAndUserId(jobId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
    }

    @Transactional(readOnly = true)
    public Path getDownloadPath(UUID userId, UUID jobId) {
        VideoJob job = getJob(userId, jobId);
        if (job.getStatus() != VideoJobStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job ainda não concluído");
        }
        if (job.getOutputPath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo de saída indisponível");
        }
        Path outputPath = videoStorageService.resolveOutputPath(job.getOutputPath());
        if (!Files.exists(outputPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo de saída não encontrado");
        }
        return outputPath;
    }

    @Transactional
    @CacheEvict(cacheNames = "videoJobs", key = "#result.userId")
    public VideoJob markProcessing(UUID jobId) {
        VideoJob job = videoJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        job.markProcessing(Instant.now());
        return job;
    }

    @Transactional
    @CacheEvict(cacheNames = "videoJobs", key = "#result.userId")
    public VideoJob markCompleted(UUID jobId, String outputPath) {
        VideoJob job = videoJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        job.markCompleted(outputPath, Instant.now());
        return job;
    }

    @Transactional
    @CacheEvict(cacheNames = "videoJobs", key = "#result.userId")
    public VideoJob markFailed(UUID jobId, String errorMessage, NotificationService notificationService) {
        VideoJob job = videoJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job não encontrado"));
        job.markFailed(errorMessage, Instant.now());
        notificationService.notifyProcessingFailed(job.getId(), job.getUserId(), errorMessage);
        return job;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo de vídeo obrigatório");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do arquivo inválido");
        }
    }
}
