package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.SubDepartmentMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.DepartmentRepository;
import in.co.bel.acempp.repository.FileSystemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileBindingService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final FileSystemRepository fileAttachmentsRepo;
    private final DepartmentRepository deptRepo;
    private final MongoTemplate mongoTemplate;
    private final FileReadOnlyService fileReadOnlyService;


    public FileAttachments promoteAndBindFile(String fileId, Long subDepartmentId, String formId) {
        return promoteAndBindFile(fileId, subDepartmentId, formId, null);
    }


    public FileAttachments promoteAndBindFile(
            String fileId,
            Long subDepartmentId,
            String formId,
            FileBindingScope scope) {
        log.info("Promoting temp file to permanent storage: fileId={}, subDepartmentId={}, formId={}",
                fileId, subDepartmentId, formId);

        // 1. Fetch Mongo record
        FileAttachments attachment = fileAttachmentsRepo
                .findBySubDepartmentIdAndFileId(subDepartmentId, fileId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FILE_NOT_FOUND,
                        "File attachment record not found for fileId: " + fileId
                ));

        // Edge Case 1: Idempotency check - file is already promoted to this form
        if ("SUCCESS".equals(attachment.getStatus()) && formId.equals(attachment.getFormId())) {
            log.info("File fileId={} is already promoted to formId={}. Skipping move.", fileId, formId);
            return attachment;
        }

        // 2. Resolve SubDepartment name for folder path
        String safeSubDept = resolveSafeSubDepartmentFolder(subDepartmentId);

        // 3. Resolve Paths
        Path tempFilePath = Paths.get(attachment.getFilePath()).normalize();
        Path permanentDir = Paths.get(uploadDir, safeSubDept).toAbsolutePath().normalize();
        Path permanentFilePath = permanentDir.resolve(attachment.getStoredFileName());

        // Edge Case 2: Validate temp file exists before moving
        if (!Files.exists(tempFilePath) && !Files.exists(permanentFilePath)) {
            log.error("Promotion failed: Source temp file missing on disk at {}", tempFilePath);
            attachment.setStatus("FAILED");
            fileAttachmentsRepo.save(attachment);
            throw new AcemException(
                    ErrorCode.FILE_NOT_FOUND,
                    "File content has expired or is missing from temp storage: " + attachment.getOriginalFileName()
            );
        }

        try {
            // Ensure permanent target directory exists
            if (Files.notExists(permanentDir)) {
                Files.createDirectories(permanentDir);
            }

            // Move file physically if still in temp
            if (Files.exists(tempFilePath)) {
                Files.move(tempFilePath, permanentFilePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Physically moved file from temp to permanent: {}", permanentFilePath);
            }

            // 4. Update MongoDB Document
            attachment.setFilePath(permanentFilePath.toString());
            attachment.setFormId(formId);
            attachment.setStatus("SUCCESS");
            applyScope(attachment, scope);

            return fileAttachmentsRepo.save(attachment);

        } catch (IOException e) {
            log.error("Failed to move file [{}] to permanent storage: {}", attachment.getOriginalFileName(), e.getMessage(), e);

            // Attempt rollback move if file ended up in permanent folder but DB write might fail
            rollbackDiskMoveQuietly(permanentFilePath, tempFilePath);

            // Update status safely
            try {
                attachment.setStatus("FAILED");
                fileAttachmentsRepo.save(attachment);
            } catch (Exception ex) {
                log.error("Could not update status to FAILED for fileId={}", fileId, ex);
            }

            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "Failed to promote file [" + attachment.getOriginalFileName() + "] to permanent folder: " + e.getMessage()
            );
        }
    }

    public void rollbackOrUnbindFile(String fileId) {
        log.info("Initiating rollback for fileId={}", fileId);

        FileAttachments attachment = fileAttachmentsRepo.findByFileId(fileId)
                .orElse(null);

        if (attachment == null) {
            log.warn("Rollback skipped: FileAttachment record not found for fileId={}", fileId);
            return;
        }

        // If file was not promoted, skip
        if (!"SUCCESS".equals(attachment.getStatus())) {
            log.info("Rollback skipped: File fileId={} is not in SUCCESS status (current status={})",
                    fileId, attachment.getStatus());
            return;
        }

        Path currentPermanentPath = Paths.get(attachment.getFilePath()).normalize();

        String subDeptFolder = currentPermanentPath.getParent().getFileName().toString();

        Path tempDir = Paths.get(uploadDir, "temp", subDeptFolder).toAbsolutePath().normalize();
        Path tempFilePath = tempDir.resolve(attachment.getStoredFileName());

        try {
            if (Files.notExists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            if (Files.exists(currentPermanentPath)) {
                Files.move(currentPermanentPath, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Successfully moved file back to temp location: {}", tempFilePath);
            } else {
                log.warn("Physical file not found at permanent path: {}", currentPermanentPath);
            }

            attachment.setFilePath(tempFilePath.toString());
            attachment.setFormId(null);
            attachment.setStatus("TEMP");
            fileAttachmentsRepo.save(attachment);

            log.info("Rollback completed for fileId={}", fileId);

        } catch (IOException e) {
            log.error("Failed to physically move file back to temp during rollback [fileId={}]: {}", fileId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to reset Mongo attachment status during rollback [fileId={}]: {}", fileId, e.getMessage(), e);
        }
    }

    private void rollbackDiskMoveQuietly(Path permanentPath, Path tempPath) {
        try {
            if (Files.exists(permanentPath) && !Files.exists(tempPath)) {
                Files.move(permanentPath, tempPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Rolled back physical file move to temp directory: {}", tempPath);
            }
        } catch (IOException e) {
            log.error("Failed to rollback file move from {} to {}", permanentPath, tempPath, e);
        }
    }

    public void purgeRemovedFilesFromDiskAndDb(String formId, List<String> fileIdsToPurge) {
        purgeRemovedFilesFromDiskAndDb(formId, null, fileIdsToPurge);
    }

    public void purgeRemovedFilesFromDiskAndDb(
            String formId,
            Long subDepartmentId,
            List<String> fileIdsToPurge) {
        if (fileIdsToPurge == null || fileIdsToPurge.isEmpty()) {
            return;
        }

        List<String> purgeableIds = fileReadOnlyService.excludeReadOnly(subDepartmentId, fileIdsToPurge);
        if (purgeableIds.isEmpty()) {
            log.info(
                    "Skipped purge of {} read-only file id(s) for formId={}",
                    fileIdsToPurge.size(),
                    formId);
            return;
        }
        if (purgeableIds.size() < fileIdsToPurge.size()) {
            log.info(
                    "Purging {} writable file(s); skipped {} read-only id(s) for formId={}",
                    purgeableIds.size(),
                    fileIdsToPurge.size() - purgeableIds.size(),
                    formId);
        }

        Criteria criteria = Criteria.where("formId").is(formId)
                .and("fileId").in(purgeableIds);
        if (subDepartmentId != null) {
            criteria = criteria.and("subDepartmentId").is(subDepartmentId);
        }

        List<FileAttachments> filesToPurge = mongoTemplate.find(
                new Query(criteria),
                FileAttachments.class,
                "file_attachments");

        for (FileAttachments file : filesToPurge) {
            try {
                if (file.isReadOnly()) {
                    log.info(
                            "Skipping purge of read-only form attachment fileId={} formId={}",
                            file.getFileId(),
                            formId);
                    continue;
                }
                if (file.getFilePath() != null && !file.getFilePath().isBlank()) {
                    Path diskPath = Paths.get(file.getFilePath());
                    boolean deleted = Files.deleteIfExists(diskPath);
                    if (deleted) {
                        log.info("Successfully deleted file from disk: {}", diskPath);
                    } else {
                        log.warn("Disk file path not found for deletion: {}", diskPath);
                    }
                }

                Criteria removeCriteria;
                if (file.getId() != null) {
                    removeCriteria = Criteria.where("id").is(file.getId());
                } else {
                    removeCriteria = Criteria.where("fileId").is(file.getFileId())
                            .and("formId").is(formId);
                    if (file.getSubDepartmentId() != null) {
                        removeCriteria = removeCriteria.and("subDepartmentId").is(file.getSubDepartmentId());
                    }
                }

                // Perform Soft Delete instead of hard removing from MongoDB
                Update update = new Update()
                        .set("isDeleted", true)
                        .set("status", "DELETED")
                        .set("deletedAt", LocalDateTime.now());

                mongoTemplate.updateFirst(new Query(removeCriteria), update, FileAttachments.class, "file_attachments");
                log.info("Successfully soft-deleted file metadata record for fileId={} formId={} subDepartmentId={}",
                        file.getFileId(), formId, file.getSubDepartmentId());

            } catch (Exception e) {
                log.error("Failed to purge removed file from disk/DB for fileId: {}", file.getFileId(), e);
            }
        }
    }

    public List<FileAttachments> getAttachmentsByFormId(String formId) {
        if (formId == null || formId.isBlank()) {
            return Collections.emptyList();
        }
        Query query = new Query(Criteria.where("formId").is(formId));
        return mongoTemplate.find(query, FileAttachments.class, "file_attachments");
    }

    private String resolveSafeSubDepartmentFolder(Long subDepartmentId) {
        String subDeptName = deptRepo.findBySubDepartmentsSubDepartmentId(subDepartmentId)
                .flatMap(dept -> dept.getSubDepartments().stream()
                        .filter(sub -> sub.getSubDepartmentId().equals(subDepartmentId))
                        .findFirst())
                .map(SubDepartmentMaster::getSubDepartmentName)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.DEPARTMENTS_NOT_FOUND,
                        "Department or Sub-Department not found for ID: " + subDepartmentId));
        return subDeptName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private void applyScope(FileAttachments attachment, FileBindingScope scope) {
        if (scope == null) {
            return;
        }
        if (scope.lotId() != null) {
            attachment.setLotId(scope.lotId());
        }
        if (scope.motorCasingId() != null) {
            attachment.setMotorCasingId(scope.motorCasingId());
        }
        if (scope.motorId() != null) {
            attachment.setMotorId(scope.motorId());
        }
        if (scope.premixNo() != null) {
            attachment.setPremixNo(scope.premixNo());
        }
        if (scope.stageType() != null) {
            attachment.setStageType(scope.stageType());
        }
        if (scope.divisionId() != null) {
            attachment.setDivisionId(scope.divisionId());
        }
        if (scope.division() != null) {
            attachment.setDivision(scope.division());
        }
        if (scope.subType() != null) {
            attachment.setSubType(scope.subType());
        }
        if (scope.stfTestNo() != null) {
            attachment.setStfTestNo(scope.stfTestNo());
        }
        if (scope.bemNo() != null) {
            attachment.setBemNo(scope.bemNo());
        }
    }

    public String getFileHash(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return fileAttachmentsRepo.findByFileId(fileId)
                .map(FileAttachments::getFileHash)
                .orElse(null);
    }
}
