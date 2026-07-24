package br.com.fiapx.api.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);

    @Override
    public void notifyProcessingFailed(UUID jobId, UUID userId, String errorMessage) {
        log.error("Falha no processamento do job {} do usuário {}: {}", jobId, userId, errorMessage);
    }
}
