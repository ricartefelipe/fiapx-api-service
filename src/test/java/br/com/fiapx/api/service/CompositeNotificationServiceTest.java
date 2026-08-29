package br.com.fiapx.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class CompositeNotificationServiceTest {

    @Mock
    private LogNotificationService logNotificationService;

    @Mock
    private ObjectProvider<EmailNotificationService> emailNotificationService;

    @InjectMocks
    private CompositeNotificationService compositeNotificationService;

    @Test
    void shouldNotifyLogAndEmailWhenAvailable() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmailNotificationService emailService = mock(EmailNotificationService.class);
        doAnswer(invocation -> {
            Consumer<EmailNotificationService> consumer = invocation.getArgument(0);
            consumer.accept(emailService);
            return null;
        }).when(emailNotificationService).ifAvailable(any());

        compositeNotificationService.notifyProcessingFailed(jobId, userId, "erro");

        verify(logNotificationService).notifyProcessingFailed(jobId, userId, "erro");
        verify(emailService).notifyProcessingFailed(jobId, userId, "erro");
    }

    @Test
    void shouldNotifyLogOnlyWhenEmailUnavailable() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doAnswer(invocation -> null).when(emailNotificationService).ifAvailable(any());

        compositeNotificationService.notifyProcessingFailed(jobId, userId, "erro");

        verify(logNotificationService).notifyProcessingFailed(jobId, userId, "erro");
    }
}
