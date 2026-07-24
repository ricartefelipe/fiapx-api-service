package br.com.fiapx.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_jobs")
public class VideoJob {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoJobStatus status;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "output_path")
    private String outputPath;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VideoJob() {
    }

    public VideoJob(UUID id, UUID userId, String originalFilename, VideoJobStatus status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public VideoJobStatus getStatus() {
        return status;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markQueued(String storagePath, Instant updatedAt) {
        this.status = VideoJobStatus.QUEUED;
        this.storagePath = storagePath;
        this.updatedAt = updatedAt;
    }

    public void markProcessing(Instant updatedAt) {
        this.status = VideoJobStatus.PROCESSING;
        this.updatedAt = updatedAt;
    }

    public void markCompleted(String outputPath, Instant updatedAt) {
        this.status = VideoJobStatus.COMPLETED;
        this.outputPath = outputPath;
        this.updatedAt = updatedAt;
    }

    public void markFailed(String errorMessage, Instant updatedAt) {
        this.status = VideoJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = updatedAt;
    }
}
