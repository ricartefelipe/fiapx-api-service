package br.com.fiapx.api.service;

import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CompositeNotificationService implements NotificationService {

    private final LogNotificationService logNotificationService;
    private final ObjectProvider<EmailNotificationService> emailNotificationService;

    public CompositeNotificationService(
        LogNotificationService logNotificationService,
        ObjectProvider<EmailNotificationService> emailNotificationService
    ) {
        this.logNotificationService = logNotificationService;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    public void notifyProcessingFailed(UUID jobId, UUID userId, String errorMessage) {
        logNotificationService.notifyProcessingFailed(jobId, userId, errorMessage);
        emailNotificationService.ifAvailable(service -> service.notifyProcessingFailed(jobId, userId, errorMessage));
    }
}
