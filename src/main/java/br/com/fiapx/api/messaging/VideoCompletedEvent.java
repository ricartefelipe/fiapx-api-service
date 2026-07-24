package br.com.fiapx.api.messaging;

import java.util.UUID;

public record VideoCompletedEvent(UUID jobId, String outputPath) {
}
