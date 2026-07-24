package br.com.fiapx.api.service;

import br.com.fiapx.api.domain.User;
import br.com.fiapx.api.domain.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    public EmailNotificationService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    @Override
    public void notifyProcessingFailed(UUID jobId, UUID userId, String errorMessage) {
        userRepository.findById(userId).ifPresent(user -> sendEmail(user, jobId, errorMessage));
    }

    private void sendEmail(User user, UUID jobId, String errorMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("FIAP X — falha no processamento do vídeo " + jobId);
        message.setText(
            "Olá " + user.getUsername() + ",\n\n"
                + "O processamento do vídeo " + jobId + " falhou.\n"
                + "Motivo: " + errorMessage + "\n\n"
                + "Consulte GET /api/videos/" + jobId + " para detalhes."
        );
        mailSender.send(message);
        log.info("E-mail de falha enviado para {} (job {})", user.getEmail(), jobId);
    }
}
