package in.co.bel.acempp.validation;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class FileUploadPathValidator {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void validateUploadDirectory() {
        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            throw new IllegalStateException("Property 'file.upload-dir' is not configured in properties file.");
        }

        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        File directory = path.toFile();

        log.info("Validating file upload directory at: {}", path);

        // Create the configured directory when it is missing so fresh deployments
        // and test environments do not need a machine-specific setup step.
        if (!directory.exists()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                String errorMsg = String.format("CRITICAL: Could not create upload directory '%s'.", path);
                log.error(errorMsg, e);
                throw new IllegalStateException(errorMsg, e);
            }
        }

        // 2. Check if it's actually a directory
        if (!directory.isDirectory()) {
            String errorMsg = String.format("CRITICAL: Configured path '%s' exists but is NOT a directory!", path);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // 3. Check read/write permissions
        if (!directory.canRead() || !directory.canWrite()) {
            String errorMsg = String.format("CRITICAL: Application lacks read/write permissions for upload directory '%s'!", path);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        log.info("Upload directory successfully verified: {}", path);
    }
}
