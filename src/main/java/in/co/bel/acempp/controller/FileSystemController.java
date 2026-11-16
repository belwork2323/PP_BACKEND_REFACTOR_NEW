package in.co.bel.acempp.controller;

import in.co.bel.acempp.dto.AcemResponse;
import in.co.bel.acempp.dto.file.*;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.exception.AcemTemplate;
import in.co.bel.acempp.service.FileSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileSystemController {

    private final FileSystemService fileSystemService;

//    @PostMapping("/upload")
//    public ResponseEntity<AcemResponse<FileAttachments>> uploadFile(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("subDepartmentId") Integer subDepartmentId,
//            @RequestParam("fileId") String fileId,
//            Authentication authentication) {
//
//        String userId = (authentication != null && authentication.getDetails() != null)
//                ? (String) authentication.getDetails()
//                : "SYSTEM";
//
//        log.info("Received request to upload file. fileId={}, subDepartmentId={}, fileName={}, size={} bytes, userId={}",
//                  fileId, subDepartmentId, file.getOriginalFilename(), file.getSize(), userId);
//
//        FileAttachments uploaded = fileSystemService.uploadSingleFileParallel(
//                file, subDepartmentId,   fileId,  userId);
//
//        log.info("Successfully processed file upload. fileId={}, storedFileName={}", fileId, uploaded.getStoredFileName());
//
//        return AcemTemplate.success(uploaded, "File uploaded successfully");
//    }
//
//    @PostMapping("/status")
//    public ResponseEntity<AcemResponse<List<FileStatusResponse>>> getFileStatuses(
//            @RequestBody FileStatusRequest request) {
//
//        log.info("Received request to fetch file statuses. subDepartmentId={}, fileCount={}",
//                request.getSubDepartmentId(),
//                request.getFileId() != null ? request.getFileId().size() : 0);
//
//        List<FileStatusResponse> fileStatuses = fileSystemService.getFileStatuses(
//                request.getSubDepartmentId(),
//                request.getFileId()
//        );
//
//        log.info("Retrieved {} file status records for subDepartmentId={}", fileStatuses.size(), request.getSubDepartmentId());
//
//        return AcemTemplate.success(fileStatuses, "File statuses retrieved successfully");
//    }

    @PostMapping("/download")
    public ResponseEntity<AcemResponse<FileDownloadResponse>> downloadFile(
            @RequestBody FileStatusRequest request) {

        String fileId = request.getSingleFileId();
        log.info("Received request to download file as Base64. subDepartmentId={}, fileId={}", request.getSubDepartmentId(), fileId);

        if (fileId == null) {
            log.warn("Download request rejected: fileIds list is null or empty. subDepartmentId={}", request.getSubDepartmentId());
            throw new AcemException(ErrorCode.BAD_REQUEST, "fileIds cannot be empty.");
        }

        FileDownloadResponse responseData = fileSystemService.downloadFileAsBase64(
                request.getSubDepartmentId(),
                fileId,
                request.getMotorId()
        );

        log.info("Successfully prepared Base64 payload for fileId={}", fileId);

        return AcemTemplate.success(responseData, "File downloaded successfully");
    }

    @PostMapping("/download/stream")
    public ResponseEntity<Resource> streamFile(@RequestBody FileStatusRequest request) {

        String fileId = request.getSingleFileId();
        log.info("Received request to stream binary file content. subDepartmentId={}, fileId={}", request.getSubDepartmentId(), fileId);

        if (fileId == null) {
            log.warn("Stream download request rejected: fileIds list is null or empty. subDepartmentId={}", request.getSubDepartmentId());
            throw new AcemException(ErrorCode.BAD_REQUEST, "fileIds cannot be empty.");
        }

        FileAttachments metadata = fileSystemService.getFileMetadata(
                request.getSubDepartmentId(), fileId, request.getMotorId());
        Resource resource = fileSystemService.loadFileAsResource(
                request.getSubDepartmentId(), fileId, request.getMotorId());

        String contentType = (metadata.getMimeType() != null) ? metadata.getMimeType() : "application/octet-stream";

        log.info("Streaming file content for fileId={}, fileName={}, mimeType={}", fileId, metadata.getOriginalFileName(), contentType);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                .header("X-Acem-Response-Status", "SUCCESS")
                .header("X-Acem-Message", "File downloaded successfully")
                .body(resource);
    }


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AcemResponse<TempUploadResponseDTO>> uploadSingleTempFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("subDepartmentId") Long subDepartmentId,
            Authentication authentication) {

        String userId = (authentication != null && authentication.getDetails() != null)
                ? (String) authentication.getDetails()
                : "SYSTEM";

        log.info("Received request to upload batch  subDepartmentId={}, userId={}",
                subDepartmentId, userId);

        TempUploadResponseDTO responseData = fileSystemService.uploadSingleTempFile(
                file,
                subDepartmentId,
                userId
        );
        log.info("Successfully processed batch file upload of {} files for subDepartmentId={}",
                responseData, subDepartmentId);

        return AcemTemplate.success(responseData, "Files uploaded successfully");
    }

    @PostMapping("/delete/temp")
    public ResponseEntity<AcemResponse<String>> deleteTempFile(
            @RequestBody FileStatusRequest request,
            Authentication authentication) {

        String userId = (authentication != null && authentication.getDetails() != null)
                ? (String) authentication.getDetails()
                : "SYSTEM";
        String fileId = request.getSingleFileId();

        log.info("Received request to delete TEMP file. subDepartmentId={}, fileId={}, userId={}",
                request.getSubDepartmentId(), fileId, userId);

        fileSystemService.deleteTempFile(request.getSubDepartmentId(), fileId);

        log.info("Successfully deleted temp fileId={} for subDepartmentId={}", fileId, request.getSubDepartmentId());

        return AcemTemplate.success(null, "Temporary file deleted successfully");
    }


    @PostMapping("/delete")
    public ResponseEntity<AcemResponse<String>> deletePermanentFile(
            @RequestBody FileStatusRequest request,
            Authentication authentication) {

        String userId = (authentication != null && authentication.getDetails() != null)
                ? (String) authentication.getDetails()
                : "SYSTEM";
        String fileId = request.getSingleFileId();

        log.info("Received request to delete PERMANENT file. subDepartmentId={}, fileId={}, userId={}",
                request.getSubDepartmentId(), fileId, userId);

        fileSystemService.deletePermanentFile(request.getSubDepartmentId(), fileId);

        log.info("Successfully deleted permanent fileId={} for subDepartmentId={}", fileId, request.getSubDepartmentId());

        return AcemTemplate.success(null, "Permanent file deleted successfully");
    }


}