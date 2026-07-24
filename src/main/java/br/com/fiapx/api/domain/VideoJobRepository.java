package br.com.fiapx.api.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoJobRepository extends JpaRepository<VideoJob, UUID> {

    List<VideoJob> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<VideoJob> findByIdAndUserId(UUID id, UUID userId);
}
