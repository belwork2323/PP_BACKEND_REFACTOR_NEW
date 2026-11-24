package in.co.bel.acempp.config;

import in.co.bel.acempp.entity.FileUploadConfig;
import in.co.bel.acempp.enums.FileCategory;
import in.co.bel.acempp.repository.FileUploadConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures PDF / IMAGE / VIDEO upload configs allow at least 50MB so limits match
 * UI copy and {@code spring.servlet.multipart.max-file-size}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadConfigBootstrap {

    /** Matches FE MAX_SIZE_MB and spring.servlet.multipart.max-file-size=50MB. */
    public static final long MAX_STANDARD_UPLOAD_BYTES = 50L * 1024 * 1024;

    private static final List<FileCategory> STANDARD_CATEGORIES =
            List.of(FileCategory.PDF, FileCategory.IMAGE, FileCategory.VIDEO);

    private final FileUploadConfigRepository configRepository;
    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureStandardUploadLimits() {
        for (FileCategory category : STANDARD_CATEGORIES) {
            upsertMaxSize(category);
        }
    }

    private void upsertMaxSize(FileCategory category) {
        FileUploadConfig config = mongoTemplate.findOne(
                Query.query(Criteria.where("category").is(category)),
                FileUploadConfig.class);

        if (config == null) {
            config = new FileUploadConfig();
            config.setCategory(category);
            config.setIsActive(true);
            config.setAllowedExtensions(defaultExtensions(category));
            config.setAllowedMimeTypes(defaultMimeTypes(category));
            config.setMinSizeBytes(0L);
            config.setMaxSizeBytes(MAX_STANDARD_UPLOAD_BYTES);
            configRepository.save(config);
            log.info("Created FileUploadConfig for {} with maxSizeBytes={}", category, MAX_STANDARD_UPLOAD_BYTES);
            return;
        }

        Long currentMax = config.getMaxSizeBytes();
        if (currentMax == null || currentMax < MAX_STANDARD_UPLOAD_BYTES) {
            config.setMaxSizeBytes(MAX_STANDARD_UPLOAD_BYTES);
            if (config.getIsActive() == null) {
                config.setIsActive(true);
            }
            configRepository.save(config);
            log.info(
                    "Updated FileUploadConfig for {} maxSizeBytes {} -> {}",
                    category,
                    currentMax,
                    MAX_STANDARD_UPLOAD_BYTES);
        }
    }

    private static List<String> defaultExtensions(FileCategory category) {
        return switch (category) {
            case PDF -> List.of("pdf");
            case IMAGE -> List.of("jpg", "jpeg", "png", "webp", "gif");
            case VIDEO -> List.of("mp4", "webm", "mov");
            default -> List.of();
        };
    }

    private static List<String> defaultMimeTypes(FileCategory category) {
        return switch (category) {
            case PDF -> List.of("application/pdf");
            case IMAGE -> List.of("image/jpeg", "image/png", "image/webp", "image/gif");
            case VIDEO -> List.of("video/mp4", "video/webm", "video/quicktime");
            default -> List.of();
        };
    }
}
