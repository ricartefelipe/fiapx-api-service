package br.com.fiapx.api.messaging;

import java.util.UUID;

public record VideoFailedEvent(UUID jobId, String errorMessage) {
}
