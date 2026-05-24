package interview_prep.profile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path avatarsDir;

    public AvatarStorageService(@Value("${app.storage.avatars-dir}") String avatarsDir) {
        this.avatarsDir = Path.of(avatarsDir).toAbsolutePath().normalize();
    }

    public String save(MultipartFile file, String oldFileName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Avatar must be jpeg, png or webp");
        }

        try {
            Files.createDirectories(avatarsDir);
            String fileName = UUID.randomUUID() + extension(contentType);
            Path target = avatarsDir.resolve(fileName).normalize();
            if (!target.startsWith(avatarsDir)) {
                throw new IllegalArgumentException("Invalid avatar file name");
            }
            file.transferTo(target);
            delete(oldFileName);
            return fileName;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not store avatar");
        }
    }

    public Resource load(String fileName) {
        try {
            Path target = avatarsDir.resolve(fileName).normalize();
            if (!target.startsWith(avatarsDir) || !Files.exists(target)) {
                throw new IllegalArgumentException("Avatar not found");
            }
            return new UrlResource(target.toUri());
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Avatar not found");
        }
    }

    public void delete(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Path target = avatarsDir.resolve(fileName).normalize();
            if (target.startsWith(avatarsDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
            // Avatar cleanup should not break profile updates.
        }
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Avatar must be jpeg, png or webp");
        };
    }
}
