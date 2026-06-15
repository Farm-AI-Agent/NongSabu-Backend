package com.nongsabu.backend.infra.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalStorageService {

    private final Path rootPath;

    public LocalStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(rootPath);
    }

    public String store(String category, MultipartFile file) throws IOException {
        Path categoryPath = rootPath.resolve(category);
        Files.createDirectories(categoryPath);
        String filename = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
        Path target = categoryPath.resolve(filename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target.toString();
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file.bin";
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

