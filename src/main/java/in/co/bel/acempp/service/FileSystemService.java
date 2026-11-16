package in.co.bel.acempp.service;

import com.mongodb.client.result.UpdateResult;
import in.co.bel.acempp.dto.file.FileDownloadResponse;
import in.co.bel.acempp.dto.file.TempUploadResponseDTO;
import in.co.bel.acempp.entity.DepartmentMaster;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.FileUploadConfig;
import in.co.bel.acempp.entity.SubDepartmentMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.FileCategory;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.DepartmentRepository;
import in.co.bel.acempp.repository.FileSystemRepository;
import in.co.bel.acempp.repository.FileUploadConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSystemService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final FileSystemRepository fileRepository;
    private final MongoTemplate mongoTemplate;
    private final FileUploadConfigRepository configRepository;
    private final DepartmentRepository deptRepo;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileAttachmentLookupService fileAttachmentLookupService;

    private Path getTempDirectory(String subDeptName) throws IOException {
        String safeSubDept = sanitizeFolderName(subDeptName);

        Path tempPath = Paths.get(uploadDir, "temp", safeSubDept)
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }

        return tempPath;
    }


    private void validateFileAgainstCategoryConfig(MultipartFile file, String fileName, FileCategory category) {
        if (category == null) {
            log.warn("Validation failed: Unsupported or unknown file category for file '{}'", fileName);
            throw new AcemException(ErrorCode.BAD_REQUEST, "Unsupported file format for upload.");
        }

        FileUploadConfig config = configRepository.findByCategoryAndIsActiveTrue(category)
                .orElseThrow(() -> {
                    log.error("No active FileUploadConfig found in DB for category={}", category);
                    return new AcemException(ErrorCode.BAD_REQUEST,
                            "Upload configuration for file type " + category + " is not configured in DB.");
                });

        long fileSize = file.getSize();
        String fileExtension = getFileExtension(fileName).toLowerCase();

        if (config.getMinSizeBytes() != null && fileSize < config.getMinSizeBytes()) {
            log.warn("File validation failed (size too small): fileName={}, size={} bytes, minRequired={} bytes",
                    fileName, fileSize, config.getMinSizeBytes());
            throw new AcemException(ErrorCode.BAD_REQUEST,
                    "File size too small. Minimum required: " + (config.getMinSizeBytes() / 1024) + " KB");
        }

        if (config.getMaxSizeBytes() != null && fileSize > config.getMaxSizeBytes()) {
            log.warn("File validation failed (size exceeded): fileName={}, size={} bytes, maxAllowed={} bytes",
                    fileName, fileSize, config.getMaxSizeBytes());
            throw new AcemException(ErrorCode.BAD_REQUEST,
                    "File size exceeds max limit of " + (config.getMaxSizeBytes() / (1024 * 1024)) + " MB");
        }

        if (config.getAllowedExtensions() != null && !config.getAllowedExtensions().isEmpty()) {
            boolean validExt = config.getAllowedExtensions().stream()
                    .anyMatch(ext -> ext.equalsIgnoreCase(fileExtension));
            if (!validExt) {
                log.warn("File validation failed (invalid extension): fileName={}, extension={}, allowed={}",
                        fileName, fileExtension, config.getAllowedExtensions());
                throw new AcemException(ErrorCode.BAD_REQUEST,
                        "Invalid file format '." + fileExtension + "' for " + category + ". Allowed: " + config.getAllowedExtensions());
            }
        }
    }

    private FileCategory detectCategory(String fileName, String mimeType) {
        String ext = getFileExtension(fileName).toLowerCase();

        // 1. PDF
        if ("pdf".equals(ext) || (mimeType != null && mimeType.contains("pdf"))) {
            return FileCategory.PDF;
        }

        // 2. Images (JPG, JPEG, PNG, WEBP, GIF, SVG)
        if (ext.matches("jpg|jpeg|png|webp|gif|svg") || (mimeType != null && mimeType.startsWith("image/"))) {
            return FileCategory.IMAGE;
        }

        // 3. Videos (MP4, WEBM, MKV, AVI)
        if (ext.matches("mp4|webm|mkv|avi") || (mimeType != null && mimeType.startsWith("video/"))) {
            return FileCategory.VIDEO;
        }

        // 4. Office Documents (Word, Excel, PowerPoint)
        if (ext.matches("doc|docx|xls|xlsx|ppt|pptx")) {
            return FileCategory.OFFICE;
        }

        // 5. Compressed Archives (ZIP, 7Z, RAR, TAR, GZ)
        if (ext.matches("zip|7z|rar|tar|gz") || (mimeType != null && mimeType.contains("zip"))) {
            return FileCategory.ZIP;
        }

        // 6. Graphs & Data
        if (ext.matches("csv|json|xml")) {
            return FileCategory.GRAPH;
        }

        return null;
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return (lastIndex == -1) ? "" : fileName.substring(lastIndex + 1);
    }


    public FileDownloadResponse downloadFileAsBase64(Long subDepartmentId, String fileId, String motorId) {
        log.debug("Processing Base64 download for subDepartmentId={}, fileId={}, motorId={}",
                subDepartmentId, fileId, motorId);

        FileAttachments metadata = getFileMetadata(subDepartmentId, fileId, motorId);
        Resource resource = loadFileAsResource(subDepartmentId, fileId, motorId);

        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            String base64Content = Base64.getEncoder().encodeToString(bytes);

            log.info("Successfully encoded file content to Base64 for fileId={}. Payload size={} chars", fileId, base64Content.length());

            return FileDownloadResponse.builder()
                    .fileId(metadata.getFileId())
                    .originalFileName(metadata.getOriginalFileName())
                    .mimeType(metadata.getMimeType())
                    .fileSize(metadata.getFileSize())
                    .base64Content(base64Content)
                    .build();
        } catch (IOException e) {
            log.error("Failed to read file content for Base64 conversion. fileId={}, error={}", fileId, e.getMessage(), e);
            throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to read file content: " + e.getMessage());
        }
    }
    public FileAttachments getFileMetadata(Long subDepartmentId, String fileId, String motorId) {
        log.debug("Fetching metadata from MongoDB for subDepartmentId={}, fileId={}, motorId={}",
                subDepartmentId, fileId, motorId);

        Long lookupSubDept = subDepartmentId != null && subDepartmentId != 11L ? subDepartmentId : null;
        FileAttachments uploadedFile = fileAttachmentLookupService
                .resolve(fileId, lookupSubDept, motorId)
                .orElse(null);

        if (uploadedFile == null) {
            log.warn("File metadata query returned no record for subDepartmentId={}, fileId={}, motorId={}",
                    subDepartmentId, fileId, motorId);
            throw new AcemException(ErrorCode.FILE_NOT_FOUND, "File not found.");
        }

        return uploadedFile;
    }

    public Resource loadFileAsResource(Long subDepartmentId, String fileId, String motorId) {
        log.debug("Loading file binary resource from disk for subDepartmentId={}, fileId={}, motorId={}",
                subDepartmentId, fileId, motorId);

        FileAttachments uploadedFile = getFileMetadata(subDepartmentId, fileId, motorId);

        try {
            Path filePath = Paths.get(uploadedFile.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.debug("File resource located and readable on disk at: {}", filePath);
                return resource;
            } else {
                log.error("File resource exists in MongoDB but is missing or unreadable on disk at path: {}", uploadedFile.getFilePath());
                throw new AcemException(ErrorCode.FILE_NOT_FOUND, "File content not found on disk at: " + uploadedFile.getFilePath());
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL path encountered for fileId={}. Path={}, error={}", fileId, uploadedFile.getFilePath(), e.getMessage(), e);
            throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR, "Invalid file path format: " + e.getMessage());
        }
    }

    public TempUploadResponseDTO uploadSingleTempFile(
            MultipartFile file,
            Long subDepartmentId,
            String userId) {

        TempUploadResponseDTO dto = new TempUploadResponseDTO();

        // 1. Basic null/empty file check
        if (file == null || file.isEmpty()) {
            dto.setStatus("FAILED");
            dto.setErrorMessage("Selected file is empty.");
            return dto;
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            dto.setStatus("FAILED");
            dto.setErrorMessage("Original filename is missing.");
            return dto;
        }

        try {
            // 2. Detect category & validate against DB upload config
            FileCategory detectedCategory = detectCategory(originalFileName, file.getContentType());
            if (detectedCategory == null) {
                log.warn("Upload rejected: Unable to determine file category for fileName={}", originalFileName);
                throw new AcemException(ErrorCode.BAD_REQUEST, "Unsupported file format for upload.");
            }

            // Throws AcemException (BAD_REQUEST) if size or extension bounds fail
            validateFileAgainstCategoryConfig(file, originalFileName, detectedCategory);

            // 3. Resolve SubDepartment & target directory
            Optional<DepartmentMaster> deptOptional = deptRepo.findBySubDepartmentsSubDepartmentId(subDepartmentId);
            if (deptOptional.isEmpty()) {
                dto.setStatus("FAILED");
                dto.setErrorMessage("SubDepartment not found for ID: " + subDepartmentId);
                return dto;
            }

            String subDeptName = deptOptional.get().getSubDepartments().stream()
                    .filter(sub -> sub.getSubDepartmentId().equals(subDepartmentId))
                    .findFirst()
                    .map(SubDepartmentMaster::getSubDepartmentName)
                    .orElse("batch");

            // 4. Generate unique IDs and build paths
            String generatedFileId = "FILE_" + UUID.randomUUID().toString();
            dto.setFileId(generatedFileId);
            dto.setOriginalFileName(originalFileName);

            Path tempDir = getTempDirectory(subDeptName);
            String storedFileName = generatedFileId + "_" + sanitizeFileName(originalFileName);
            Path targetTempPath = tempDir.resolve(storedFileName);

            // 5. Transfer binary bytes to temporary storage disk
            file.transferTo(targetTempPath.toFile());

            // 6. Calculate checksum & persist MongoDB metadata
            String fileHash = calculateMD5(targetTempPath);

            FileAttachments attachment = new FileAttachments();
            attachment.setSubDepartmentId(subDepartmentId);
            attachment.setFileId(generatedFileId);
            attachment.setOriginalFileName(originalFileName);
            attachment.setStoredFileName(storedFileName);
            attachment.setFilePath(targetTempPath.toString());
            attachment.setMimeType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setFileHash(fileHash);
            attachment.setStatus("TEMP_SUCCESS");
            attachment.setUploadedAt(LocalDateTime.now());
            attachment.setUploadedBy(userId);
            attachment.setFormId(null);

            fileRepository.save(attachment);

            // 7. Populate success response
            dto.setStatus("TEMP_SUCCESS");
            dto.setTempFilePath(targetTempPath.toString());
            dto.setFileSize(file.getSize());
            dto.setMimeType(file.getContentType());

            log.info("Successfully uploaded temp file: fileId={}, category={}, size={} bytes",
                    generatedFileId, detectedCategory, file.getSize());

            return dto;

        } catch (AcemException e) {
            log.warn("File upload validation/business error: {}", e.getMessage());
            dto.setStatus("FAILED");
            dto.setErrorMessage(e.getMessage());
            return dto;
        } catch (Exception e) {
            log.error("Failed to store file in temporary directory for subDepartmentId={}: {}",
                    subDepartmentId, e.getMessage(), e);
            dto.setStatus("FAILED");
            dto.setErrorMessage("Failed to store file in temporary directory: " + e.getMessage());
            return dto;
        }
    }


    private String sanitizeFolderName(String name) {
        if (name == null || name.trim().isEmpty()) return "default_subdepartment";
        return name.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unnamed_file";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String calculateMD5(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = Files.readAllBytes(filePath);
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }


    public void deleteTempFile(Long subDepartmentId, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new AcemException(ErrorCode.BAD_REQUEST, "fileId cannot be empty");
        }

        Query query = new Query(
                Criteria.where("subDepartmentId").is(subDepartmentId)
                        .and("fileId").is(fileId)
        );

        FileAttachments uploadedFile = mongoTemplate.findOne(query, FileAttachments.class, "file_attachments");

        if (uploadedFile == null) {
            log.warn("Temp file deletion failed: Metadata not found for subDepartmentId={}, fileId={}",
                    subDepartmentId, fileId);
            throw new AcemException(ErrorCode.FILE_NOT_FOUND, "File not found for fileId: " + fileId);
        }

        fileReadOnlyService.assertWritable(subDepartmentId, fileId);

        // Safety Guard: Ensure file is not already bound to a submitted form
        if ("SUCCESS".equals(uploadedFile.getStatus()) ||
                (uploadedFile.getFormId() != null && !uploadedFile.getFormId().isBlank())) {
            log.warn("Security Alert: Attempted to call deleteTempFile on a promoted file. fileId={}", fileId);
            throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Cannot delete file via temp cleanup as it is already attached to a form");
        }

        // Perform Disk & DB deletion
        purgeFileFromDiskAndDb(query, uploadedFile);
    }

    public void deletePermanentFile(Long subDepartmentId, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new AcemException(ErrorCode.BAD_REQUEST, "fileId cannot be empty");
        }

        fileReadOnlyService.assertWritable(subDepartmentId, fileId);

        Query query = new Query(
                Criteria.where("subDepartmentId").is(subDepartmentId)
                        .and("fileId").is(fileId)
                        .and("isDeleted").ne(true) // Ensure it isn't already soft-deleted
        );

        Update update = new Update()
                .set("isDeleted", true)
                .set("status", "DELETED")
                .set("deletedAt", LocalDateTime.now());

        UpdateResult result = mongoTemplate.updateFirst(query, update, FileAttachments.class, "file_attachments");

        if (result.getMatchedCount() == 0) {
            log.warn("Permanent file soft-deletion failed: Metadata not found or already deleted for subDepartmentId={}, fileId={}",
                    subDepartmentId, fileId);
            throw new AcemException(ErrorCode.FILE_NOT_FOUND, "Active file not found for fileId: " + fileId);
        }

        log.info("Successfully marked file {} as deleted for subDepartmentId={}", fileId, subDepartmentId);
    }


    private void purgeFileFromDiskAndDb(Query query, FileAttachments uploadedFile) {
        if (uploadedFile.getFilePath() != null) {
            try {
                Path path = Paths.get(uploadedFile.getFilePath());
                boolean deleted = Files.deleteIfExists(path);
                if (deleted) {
                    log.info("Deleted physical file from disk path: {}", path);
                } else {
                    log.warn("File missing on disk during deletion attempt at path: {}", path);
                }
            } catch (IOException e) {
                log.error("Failed to delete physical file from disk at path={}: {}",
                        uploadedFile.getFilePath(), e.getMessage(), e);
                throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Could not delete physical file from storage: " + e.getMessage());
            }
        }

        // Perform Soft Delete in MongoDB
        Update update = new Update()
                .set("isDeleted", true)
                .set("status", "DELETED")
                .set("deletedAt", LocalDateTime.now());

        mongoTemplate.updateFirst(query, update, FileAttachments.class, "file_attachments");
        log.info("Successfully soft-deleted file metadata record in MongoDB for fileId={}", uploadedFile.getFileId());
    }


}