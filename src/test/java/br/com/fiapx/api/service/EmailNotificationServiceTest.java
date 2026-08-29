package br.com.fiapx.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.api.domain.User;
import br.com.fiapx.api.domain.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @Test
    void shouldSendEmailWhenUserExists() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("fiapx@fiapx.local");
        when(user.getUsername()).thenReturn("fiapx");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        emailNotificationService.notifyProcessingFailed(jobId, userId, "erro ffmpeg");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSkipEmailWhenUserMissing() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        emailNotificationService.notifyProcessingFailed(jobId, userId, "erro ffmpeg");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
