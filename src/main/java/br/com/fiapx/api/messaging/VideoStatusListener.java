package br.com.fiapx.api.messaging;

import br.com.fiapx.api.service.NotificationService;
import br.com.fiapx.api.service.VideoJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class VideoStatusListener {

    private static final Logger log = LoggerFactory.getLogger(VideoStatusListener.class);

    private final VideoJobService videoJobService;
    private final NotificationService notificationService;

    public VideoStatusListener(VideoJobService videoJobService, NotificationService notificationService) {
        this.videoJobService = videoJobService;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue-completed}")
    public void handleCompleted(VideoCompletedEvent event) {
        log.info("Job {} concluído", event.jobId());
        videoJobService.markCompleted(event.jobId(), event.outputPath());
    }

    @RabbitListener(queues = "${app.rabbitmq.queue-failed}")
    public void handleFailed(VideoFailedEvent event) {
        log.warn("Job {} falhou: {}", event.jobId(), event.errorMessage());
        videoJobService.markFailed(event.jobId(), event.errorMessage(), notificationService);
    }
}
