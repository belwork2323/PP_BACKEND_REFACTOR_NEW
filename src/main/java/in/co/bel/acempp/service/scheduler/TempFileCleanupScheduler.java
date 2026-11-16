package in.co.bel.acempp.service.scheduler;

import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.repository.FileSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TempFileCleanupScheduler {

    private final FileSystemRepository fileAttachmentsRepository;

    /**
     * Runs daily at 2:00 AM to clean up unpromoted/orphaned temporary files older than 24 hours.
     */
    public void cleanupOrphanedFiles() {
        log.info("Starting scheduled cleanup for temp and orphaned storage files...");

        // 1. Cutoff threshold: 24 hours ago
        Instant cutoffInstant = Instant.now().minus(24, ChronoUnit.HOURS);

        // Fetch all candidate records for evaluation
        List<FileAttachments> allAttachments = fileAttachmentsRepository.findAll();

        int deletedCount = 0;

        for (FileAttachments file : allAttachments) {
            boolean shouldDelete = false;

            // Convert the stored local timestamp back to an instant for cutoff comparison.
            Instant fileUploadTime = parseUploadTime(file.getUploadedAt());
            if (fileUploadTime == null || !fileUploadTime.isBefore(cutoffInstant)) {
                continue; // File is not older than 24 hours yet
            }

            String filePath = file.getFilePath();
            boolean isTempDirectory = filePath != null && filePath.contains("/temp/") || filePath.contains("\\temp\\");

            // --- CONDITION 1: Temp directory clean-up ---
            if (isTempDirectory) {
                // Any file in temp directory uploaded > 24 hours ago
                shouldDelete = true;
                log.info("Flagged for deletion [Temp Directory Expired > 24h]: fileId={}", file.getFileId());
            }
            // --- CONDITION 2: Permanent directory clean-up ---
            else if (file.getFormId() == null || file.getFormId().isBlank()) {
                // File in actual storage uploaded > 24 hours ago but formID is still null
                shouldDelete = true;
                log.info("Flagged for deletion [Actual Storage - Missing formId > 24h]: fileId={}", file.getFileId());
            }

            if (shouldDelete) {
                if (deletePhysicalAndDbRecord(file)) {
                    deletedCount++;
                }
            }
        }

        log.info("Cleanup task finished. Total deleted records: {}", deletedCount);
    }

    private boolean deletePhysicalAndDbRecord(FileAttachments file) {
        try {
            // 1. Delete physical file from disk
            if (file.getFilePath() != null) {
                Path path = Paths.get(file.getFilePath());
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("Deleted physical file from disk: {}", path.toAbsolutePath());
                }
            }

            // 2. Delete MongoDB metadata entry
            fileAttachmentsRepository.delete(file);
            return true;

        } catch (IOException e) {
            log.error("Failed to delete physical file on disk for file ID {}: {}", file.getFileId(), file.getFilePath(), e);
        } catch (Exception e) {
            log.error("Failed to clean up MongoDB record for file ID {}", file.getFileId(), e);
        }
        return false;
    }

    private Instant parseUploadTime(LocalDateTime uploadedAt) {
        return uploadedAt == null
                ? null
                : uploadedAt.atZone(ZoneId.systemDefault()).toInstant();
    }
}