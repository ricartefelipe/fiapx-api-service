package br.com.fiapx.api.service;

import java.util.UUID;

public interface NotificationService {

    void notifyProcessingFailed(UUID jobId, UUID userId, String errorMessage);
}
