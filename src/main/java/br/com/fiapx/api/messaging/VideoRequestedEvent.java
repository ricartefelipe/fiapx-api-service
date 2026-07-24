package br.com.fiapx.api.messaging;

import java.util.UUID;

public record VideoRequestedEvent(
    UUID jobId,
    UUID userId,
    String originalFilename,
    String storagePath
) {
}
