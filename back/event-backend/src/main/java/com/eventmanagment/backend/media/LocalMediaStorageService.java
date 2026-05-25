package com.eventmanagment.backend.media;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalMediaStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024L * 1024L;

    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Image too large (max 5MB)");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image type");
        }

        try {
            Path targetDir = Path.of("uploads", folder);
            Files.createDirectories(targetDir);

            String filename = UUID.randomUUID() + "." + extension;
            Path targetFile = targetDir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store image", e);
        }
    }

    private String extractExtension(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
