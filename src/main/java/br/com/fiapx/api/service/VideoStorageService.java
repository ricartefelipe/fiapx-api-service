package br.com.fiapx.api.service;

import br.com.fiapx.api.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoStorageService {

    private final Path uploadDir;

    public VideoStorageService(StorageProperties properties) throws IOException {
        this.uploadDir = Path.of(properties.uploadDir());
        Files.createDirectories(uploadDir);
    }

    public String store(UUID jobId, MultipartFile file) throws IOException {
        String extension = extractExtension(file.getOriginalFilename());
        Path destination = uploadDir.resolve(jobId + extension);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination.toAbsolutePath().toString();
    }

    public Path resolveOutputPath(String outputPath) {
        return Path.of(outputPath);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".mp4";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
