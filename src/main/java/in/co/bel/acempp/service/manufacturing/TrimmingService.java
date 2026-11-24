package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalRequest;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalResponse;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.dto.trimming.*;
import in.co.bel.acempp.dto.trimming.trimmingcreation.MotorDTO;
import in.co.bel.acempp.dto.trimming.trimmingcreation.SectionDTO;
import in.co.bel.acempp.dto.trimming.trimmingcreation.TrimmingDrawingSectionDTO;
import in.co.bel.acempp.dto.trimming.trimmingcreation.TrimmingRemarksSectionDTO;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.manufacturing.Trimming;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.manufacturing.TrimmingRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.service.BatchService;
import in.co.bel.acempp.service.batch.MainBatchGateValidator;
import in.co.bel.acempp.service.batch.MainBatchStageConstants;
import in.co.bel.acempp.service.batch.BatchMotorDiscardHelper;
import in.co.bel.acempp.service.batch.MainBatchStageProgressHelper;
import in.co.bel.acempp.service.FileBindingService;
import in.co.bel.acempp.service.FileMetadataEnrichmentService;
import in.co.bel.acempp.service.FileReadOnlyService;
import in.co.bel.acempp.service.FormIdGeneratorService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrimmingService {

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private final TrimmingRepository trimmingRepo;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final BatchService batchService;
    private final FormIdGeneratorService formIdGenerator;
    private final FileBindingService fileBindingService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;
    private final MainBatchGateValidator mainBatchGateValidator;

    public TrimmingResponse createForm(TrimmingRequest request, String userId) {
        Batch batch = getBatch(request.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());

        if (trimmingRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Trimming form already submitted for this batch");
        }

        User creator = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        String formId = formIdGenerator.generate("trimmings", "formId", "TRM", 3);

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            List<MotorDTO> motors = prepareIncomingMotors(request.getMotors(), batch);
            Status batchStatus = calculateBatchStatus(motors, totalRequiredMotors(batch));

            // 1. Promote uploaded files and track them
            promotedDocs = promoteTrimmingReportFiles(motors, request.getSubDepartmentId(), formId, Collections.emptySet());

            Trimming entity = Trimming.builder()
                    .formId(formId)
                    .batchId(request.getBatchId())
                    .batchType(batch.getBatchType())
                    .subDepartmentId(request.getSubDepartmentId())
                    .formSubmissionType(request.getFormSubmissionType())
                    .status(batchStatus)
                    .motors(motors)
                    .createdBy(PersonInfo.builder()
                            .id(userId)
                            .fullName(creator.getUsername())
                            .build())
                    .createdAt(now)
                    .build();

            Trimming savedForm = trimmingRepo.save(entity);
            updateBatchStage(
                    batch,
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    request.getFormSubmissionType() == Status.SUBMIT || hasSubmittedMotor(motors),
                    userId,
                    creator.getUsername(),
                    now);

            MotorCounts counts = countMotors(motors);
            boolean isDraft = request.getFormSubmissionType() == Status.DRAFT;
            TrimmingResponse.TrimmingResponseBuilder response = TrimmingResponse.builder()
                    .formId(savedForm.getFormId())
                    .batchId(savedForm.getBatchId())
                    .status(batchStatus)
                    .motorCount(motors.size())
                    .pendingMotorCount(counts.pending())
                    .approvedMotorCount(counts.approved())
                    .rejectedMotorCount(counts.rejected())
                    .inProgressMotorCount(counts.inProgress())
                    .totalMotorCount(totalRequiredMotors(batch))
                    .motorStatuses(buildFullMotorStatuses(batch, motors, batch.getMotorIds()));

            if (isDraft) {
                response.createdBy(userId);
            } else {
                response.submittedBy(userId)
                        .submittedAt(now);
            }

            log.info("Trimming form {} created by {}", formId, userId);
            return response.build();

        } catch (Exception ex) {
            log.error("Form creation failed for formId={}. Initiating file promotion rollback...", formId, ex);

            // Rollback promoted files if DB operation or motor preparation failed
            rollbackPromotedFiles(promotedDocs);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to create Trimming form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public TrimmingResponse updateForm(TrimmingRequest request, String userId) {
        Trimming form = trimmingRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Trimming form not found for formId: " + request.getFormId()));

        if (form.getStatus() == Status.APPROVED || form.getStatus() == Status.COMPLETELY_APPROVED) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Approved form cannot be modified");
        }
        if (!form.getBatchId().equals(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The form does not belong to batch " + request.getBatchId());
        }

        Batch batch = getBatch(form.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());
        boolean isFinalFormSubmit = request.getFormSubmissionType() == Status.SUBMIT;

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            Map<String, MotorDTO> existingMotorMap = form.getMotors() == null
                    ? new LinkedHashMap<>()
                    : form.getMotors().stream().collect(Collectors.toMap(
                    MotorDTO::getMotorId,
                    Function.identity(),
                    (first, ignored) -> first,
                    LinkedHashMap::new));

            // Validation against protected motors
            if (!isFinalFormSubmit && request.getMotors() != null) {
                for (MotorDTO incomingDto : request.getMotors()) {
                    MotorDTO existing = existingMotorMap.get(incomingDto.getMotorId());
                    if (existing != null && isProtectedMotor(existing)) {
                        throw new AcemException(
                                ErrorCode.FORM_ALREADY_EXISTS,
                                "Motor " + incomingDto.getMotorId() + " cannot be modified.");
                    }
                }
            }

            List<MotorDTO> incomingMotors = prepareIncomingMotors(request.getMotors(), batch);

            for (MotorDTO incoming : incomingMotors) {
                MotorDTO existing = existingMotorMap.get(incoming.getMotorId());
                if (existing == null || !isProtectedMotor(existing)) {
                    incoming.setRejectionReason(null);
                    existingMotorMap.put(incoming.getMotorId(), incoming);
                }
            }

            List<MotorDTO> updatedMotors = new ArrayList<>(existingMotorMap.values());
            form.setRejectionReason(resolveFormRejectionReasonFromMotors(updatedMotors));
            Status batchStatus = calculateBatchStatus(updatedMotors, totalRequiredMotors(batch));

            Set<String> existingDbFileIds = extractTrimmingFileIds(form.getMotors());

            // 2. Extract file IDs from updated incoming/merged motor structures
            Set<String> incomingFileIds = extractTrimmingFileIds(updatedMotors);

            // 3. Identify files removed by user (present in DB, absent in target state)
            List<String> removedFileIds = existingDbFileIds.stream()
                    .filter(fileId -> !incomingFileIds.contains(fileId))
                    .collect(Collectors.toList());

            // 4. Promote only NEW files
            promotedDocs = promoteTrimmingReportFiles(
                    updatedMotors,
                    request.getSubDepartmentId(),
                    request.getFormId(),
                    existingDbFileIds
            );

            // 5. Purge deleted files from disk and Mongo DB
            if (!removedFileIds.isEmpty()) {
                fileBindingService.purgeRemovedFilesFromDiskAndDb(request.getFormId(), removedFileIds);
            }
            User updater = getUser(userId);
            LocalDateTime now = LocalDateTime.now();
            form.setFormSubmissionType(request.getFormSubmissionType());
            form.setStatus(batchStatus);
            form.setSubDepartmentId(request.getSubDepartmentId());
            form.setMotors(updatedMotors);
            form.setUpdatedBy(PersonInfo.builder()
                    .id(userId)
                    .fullName(updater.getUsername())
                    .build());
            form.setUpdatedAt(now);

            Trimming savedForm = trimmingRepo.save(form);
            updateBatchStage(
                    batch,
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    isFinalFormSubmit || hasSubmittedMotor(incomingMotors),
                    userId,
                    updater.getUsername(),
                    now);

            MotorCounts counts = countMotors(updatedMotors);
            TrimmingResponse.TrimmingResponseBuilder response =
                    TrimmingResponse.builder()
                            .formId(savedForm.getFormId())
                            .batchId(savedForm.getBatchId())
                            .status(batchStatus)
                            .motorCount(updatedMotors.size())
                            .pendingMotorCount(counts.pending())
                            .approvedMotorCount(counts.approved())
                            .rejectedMotorCount(counts.rejected())
                            .inProgressMotorCount(counts.inProgress())
                            .totalMotorCount(totalRequiredMotors(batch))
                            .motorStatuses(buildFullMotorStatuses(batch, updatedMotors, batch.getMotorIds()));

            if (isFinalFormSubmit) {
                response.submittedBy(userId)
                        .submittedAt(now);
            } else {
                response.updatedBy(userId)
                        .updatedAt(now);
            }

            log.info("Trimming form {} updated by {}", savedForm.getFormId(), userId);
            return response.build();

        } catch (Exception ex) {
            log.error("Form update failed for formId={}. Initiating file promotion rollback...", request.getFormId(), ex);

            // Rollback promoted files if form update failed
            rollbackPromotedFiles(promotedDocs);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update Trimming form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public TrimmingDetailsResponse getFormDetails(String formId) {
        Trimming form = trimmingRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "No Trimming form found for formId: " + formId));
        Batch batch = getBatch(form.getBatchId());
        List<MotorDTO> motors = form.getMotors() != null ? form.getMotors() : Collections.emptyList();
        enrichTrimmingMotorFiles(motors, form.getSubDepartmentId());
        MotorCounts counts = countMotors(motors);
        int totalMotorCount = totalRequiredMotors(batch);
        PersonInfo submittedBy = form.getCreatedBy();

        return TrimmingDetailsResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .batchType(form.getBatchType())
                .createdBy(batch.getCreatedBy() != null ? batch.getCreatedBy().getId() : null)
                .createdAt(batch.getCreatedAt())
                .submittedBy(submittedBy != null ? submittedBy.getId() : null)
                .submittedByDetails(submittedBy)
                .submittedAt(form.getCreatedAt())
                .status(form.getStatus())
                .motorReceivedDateTime(motors.stream()
                        .map(MotorDTO::getMotorReceivedDateTime)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null))
                .motors(motors)
                .allMotorsApproved(counts.approved() == totalMotorCount)
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .inProgressMotorCount(counts.inProgress())
                .totalMotorCount(totalMotorCount)
                .motorStatuses(buildFullMotorStatuses(batch, motors, batch.getMotorIds()))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public MotorApprovalResponse changeStatusOfMotor(MotorApprovalRequest request, String approverUserId) {
        validateMotorApprovalRequest(request);
        Trimming form = trimmingRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Trimming form not found"));

        if (request.getSubDepartmentId() != null
                && !request.getSubDepartmentId().equals(form.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The motor approval request does not belong to trimming");
        }
        if (form.getMotors() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "No motors are available in this form");
        }

        MotorDTO motor = form.getMotors().stream()
                .filter(item -> item.getMotorId().equals(request.getMotorId()))
                .findFirst()
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor not found"));

        if (motor.getMotorSubmissionStatus() != Status.WAITING_FOR_APPROVAL) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Motor is not waiting for approval.");
        }

        motor.setMotorSubmissionStatus(request.getActionType());
        if (request.getActionType() == Status.REJECTED) {
            motor.setRejectionReason(request.getRejectionReason());
        } else {
            motor.setRejectionReason(null);
        }

        LocalDateTime now = LocalDateTime.now();
        form.setUpdatedBy(PersonInfo.builder().fullName(approverUserId).build());
        form.setUpdatedAt(now);
        form.setRemarks(request.getRemarks());
        form.setRejectionReason(resolveFormRejectionReasonFromMotors(form.getMotors()));

        Batch batch = getBatch(form.getBatchId());
        int totalMotorCount = totalRequiredMotors(batch);
        Status batchStatus = calculateBatchStatus(form.getMotors(), totalMotorCount);
        form.setStatus(batchStatus);
        if (batchStatus == Status.COMPLETELY_APPROVED) {
            form.setRejectionReason(null);
        }

        if (request.getActionType() == Status.APPROVED) {
            fileReadOnlyService.markReadOnly(form.getSubDepartmentId(), extractMotorFileIds(motor));
            try {
                Map<String, Object> blockchainPayload = buildMotorBlockchainPayload(form, motor, approverUserId, now);
                approvalBlockchainAssetService.createAssetOnBlockchain(
                        form.getSubDepartmentId(),
                        form.getBatchId(),
                        form.getFormId(),
                        blockchainPayload,
                        request.getMotorId(),
                        approverUserId);
            } catch (Exception e) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
            }
        }
        if (batchStatus == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }

        trimmingRepo.save(form);
        updateBatchAfterMotorApproval(batch, form, batchStatus, approverUserId, now);

        MotorCounts counts = countMotors(form.getMotors());
        return MotorApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .motorId(request.getMotorId())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .motorSubmissionStatus(motor.getMotorSubmissionStatus())
                .batchStatus(batchStatus)
                .allMotorsApproved(counts.approved() == totalMotorCount)
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .totalMotorCount(totalMotorCount)
                .remarks(form.getRemarks())
                .rejectionReason(form.getRejectionReason())
                .actionBy(approverUserId)
                .actionAt(now)
                .build();
    }


    private Map<String, Object> buildMotorBlockchainPayload(
            Trimming form,
            MotorDTO motor,
            String approverUserId,
            LocalDateTime approvalTime) {
        Map<String, Object> rootPayload = new LinkedHashMap<>();
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getCreatedBy() != null) {
                auditMap.put("createdBy", form.getCreatedBy().getFullName());
            }
            if (form.getCreatedAt() != null) {
                auditMap.put("createdAt", form.getCreatedAt());
            }
        }
        if (approverUserId != null) {
            auditMap.put("approvedBy", approverUserId);
            auditMap.put("approvedAt", approvalTime);
        } else if (form != null && form.getUpdatedBy() != null) {
            auditMap.put("approvedBy", form.getUpdatedBy().getFullName());
            auditMap.put("approvedAt", form.getUpdatedAt());
        }
        rootPayload.put("audit", auditMap);
        Map<String, Object> motorMap = new LinkedHashMap<>();
        if (motor != null) {
            motorMap.put("motorId", motor.getMotorId());
            motorMap.put("motorStage", motor.getMotorStage());
            motorMap.put("motorReceivedDateTime", motor.getMotorReceivedDateTime());
            if (motor.getSections() != null) {
                List<Map<String, Object>> sectionsList = new ArrayList<>();
                for (SectionDTO<?> section : motor.getSections()) {
                    Map<String, Object> sectionMap = new LinkedHashMap<>();
                    sectionMap.put("sectionId", section.getSectionId());
                    if (section instanceof TrimmingRemarksSectionDTO remarksSection) {
                        List<Map<String, Object>> processedRemarksData = new ArrayList<>();
                        if (remarksSection.getSectionData() != null) {
                            for (var remData : remarksSection.getSectionData()) {
                                Map<String, Object> remMap = new LinkedHashMap<>();
                                remMap.put("remarks", remData.getRemarks());
                                List<String> reportFileHashes = Collections.emptyList();
                                if (remData.getReportFile() != null && !remData.getReportFile().isEmpty()) {
                                    reportFileHashes = remData.getReportFile().stream()
                                            .map(DocumentDto::getFileId)
                                            .map(this::getFileHash)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
                                }
                                remMap.put("reportFile", reportFileHashes);
                                processedRemarksData.add(remMap);
                            }
                        }
                        sectionMap.put("sectionData", processedRemarksData);
                    }
                    else if (section instanceof TrimmingDrawingSectionDTO drawingSection) {
                        List<Map<String, Object>> processedDrawingData = new ArrayList<>();
                        if (drawingSection.getSectionData() != null) {
                            for (var drawData : drawingSection.getSectionData()) {
                                Map<String, Object> drawMap = new LinkedHashMap<>();
                                drawMap.put("description", drawData.getDescription());
                                List<String> drawingFileHashes = Collections.emptyList();
                                if (drawData.getDrawingFile() != null
                                        && !drawData.getDrawingFile().isEmpty()) {
                                    drawingFileHashes = drawData.getDrawingFile().stream()
                                            .map(DocumentDto::getFileId)
                                            .map(this::getFileHash)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
                                }
                                drawMap.put("drawingFile", drawingFileHashes);
                                processedDrawingData.add(drawMap);
                            }
                        }
                        sectionMap.put("sectionData", processedDrawingData);
                    }
                    else {
                        sectionMap.put(
                                "sectionData",
                                section.getSectionData() == null
                                        ? Collections.emptyList()
                                        : section.getSectionData()
                        );
                    }
                    sectionsList.add(sectionMap);
                }
                motorMap.put("sections", sectionsList);
            }
        }
        rootPayload.put("motorDetails", motorMap);
        return rootPayload;
    }

    private String getFileHash(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return fileBindingService.getFileHash(fileId);
    }

    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {
        Trimming entity = trimmingRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Trimming form not found: " + request.getFormId()));

        ApprovalStatusUtil.applyStatusChange(
                request.getActionType(),
                request.getSubDepartmentId(),
                request.getRemarks(),
                request.getRejectionReason(),
                approverName,
                entity::setStatus,
                entity::setRemarks,
                entity::setRejectionReason,
                entity::setUpdatedBy,
                entity::setUpdatedAt);

        if (request.getActionType() == Status.APPROVED
                || request.getActionType() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(entity.getFormId(), entity.getSubDepartmentId());
        }

        trimmingRepo.save(entity);

        batchService.updateBatchStatus(
                entity.getBatchId(),
                entity.getSubDepartmentId(),
                entity.getStatus(),
                entity.getRemarks(),
                entity.getRejectionReason(),
                approverName);

        return ApproverStatusResponse.builder()
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionAt(entity.getUpdatedAt())
                .actionBy(entity.getUpdatedBy())
                .build();
    }

    private List<MotorDTO> prepareIncomingMotors(List<MotorDTO> motorDtos, Batch batch) {
        if (motorDtos == null || motorDtos.isEmpty()) {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one motor is required");
        }
        if (batch.getMotorIds() == null || batch.getMotorIds().isEmpty()) {
            throw new AcemException(
                    ErrorCode.MOTORS_NOT_FOUND,
                    "Batch has no associated motors");
        }

        Map<String, MotorDTO> motorsById = new LinkedHashMap<>();
        for (MotorDTO motorDto : motorDtos) {
            if (motorDto == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Each motor form is required");
            }

            String motorId = motorDto.getMotorId();
            if (motorId == null || motorId.isBlank()) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Each motor must have a motorId");
            }
            if (!batch.getMotorIds().contains(motorId)) {
                throw new AcemException(
                        ErrorCode.MOTORS_NOT_FOUND,
                        "Motor " + motorId + " is not part of this batch");
            }
            if (motorsById.containsKey(motorId)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor " + motorId + " is duplicated in the request");
            }
            mainBatchGateValidator.assertMotorUnlocked(
                    batch, MainBatchStageConstants.TRIMMING, motorId);

            Status submissionType;
            try {
                submissionType = Status.valueOf(motorDto.getMotorSubmissionType().toUpperCase());
            } catch (Exception e) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorSubmissionType must be DRAFT or SUBMIT for motor " + motorId);
            }

            if (submissionType != Status.DRAFT && submissionType != Status.SUBMIT) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorSubmissionType must be DRAFT or SUBMIT for motor " + motorId);
            }

            // Set final calculated status directly onto MotorDTO
            motorDto.setMotorSubmissionStatus(
                    submissionType == Status.DRAFT
                            ? Status.IN_PROGRESS
                            : Status.WAITING_FOR_APPROVAL
            );
            motorDto.setRejectionReason(null);

            motorsById.put(motorId, motorDto);
        }
        return new ArrayList<>(motorsById.values());
    }

    private static String resolveFormRejectionReasonFromMotors(List<MotorDTO> motors) {
        if (motors == null) {
            return null;
        }
        return motors.stream()
                .filter(m -> m != null && m.getMotorSubmissionStatus() == Status.REJECTED)
                .map(MotorDTO::getRejectionReason)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<MotorStatusResponse> buildFullMotorStatuses(
            Batch batch,
            List<MotorDTO> savedMotors,
            List<String> batchMotorIds) {

        Map<String, MotorDTO> savedMap = savedMotors == null
                ? Collections.emptyMap()
                : savedMotors.stream().collect(Collectors.toMap(
                MotorDTO::getMotorId,
                Function.identity(),
                (first, ignored) -> first));

        List<MotorStatusResponse> statuses = new ArrayList<>();
        for (String motorId : batchMotorIds) {
            MotorDTO motor = savedMap.get(motorId);
            statuses.add(motor == null
                    ? MotorStatusResponse.builder()
                    .motorId(motorId)
                    .motorSubmissionType(Status.DRAFT)
                    .motorSubmissionStatus(MainBatchStageProgressHelper.defaultMotorSubmissionStatus(
                            batch, MainBatchStageConstants.TRIMMING, motorId))
                    .build()
                    : MotorStatusResponse.builder()
                    .motorId(motorId)
                    .motorSubmissionType(Status.valueOf(motor.getMotorSubmissionType().toUpperCase()))
                    .motorSubmissionStatus(motor.getMotorSubmissionStatus())
                    .rejectionReason(motor.getRejectionReason())
                    .build());
        }
        return BatchMotorDiscardHelper.overlayMotorStatuses(batch, statuses);
    }

    private Status calculateBatchStatus(
            List<MotorDTO> savedMotors,
            int totalRequiredMotors) {

        if (savedMotors == null || savedMotors.isEmpty()) {
            return Status.TO_BE_INITIATED;
        }
        if (savedMotors.stream().anyMatch(m -> m.getMotorSubmissionStatus() == Status.REJECTED)) {
            return Status.REJECTED;
        }

        long waitingCount = savedMotors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.WAITING_FOR_APPROVAL)
                .count();
        long approvedCount = savedMotors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.APPROVED)
                .count();

        if (totalRequiredMotors > 0
                && approvedCount == totalRequiredMotors && waitingCount == 0) {
            return Status.COMPLETELY_APPROVED;
        }
        if (waitingCount > 0) {
            if (totalRequiredMotors == 1 || (totalRequiredMotors <= 0 && savedMotors.size() == 1)) {
                return Status.WAITING_FOR_APPROVAL;
            }
            return Status.WAITING_FOR_PARTIAL_APPROVAL;
        }
        if (approvedCount > 0) {
            return Status.IN_PROGRESS;
        }
        boolean anyStarted = savedMotors.stream()
                .anyMatch(m -> m.getMotorSubmissionStatus() == Status.IN_PROGRESS
                        || "DRAFT".equalsIgnoreCase(m.getMotorSubmissionType()));
        return anyStarted ? Status.IN_PROGRESS : Status.TO_BE_INITIATED;
    }

    private MotorCounts countMotors(List<MotorDTO> motors) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int inProgress = 0;
        for (MotorDTO motor : motors) {
            Status status = motor.getMotorSubmissionStatus();
            if (status == Status.WAITING_FOR_APPROVAL) {
                pending++;
            } else if (status == Status.APPROVED) {
                approved++;
            } else if (status == Status.REJECTED) {
                rejected++;
            } else if (status == Status.IN_PROGRESS) {
                inProgress++;
            }
        }
        return new MotorCounts(pending, approved, rejected, inProgress);
    }

    private boolean isProtectedMotor(MotorDTO motor) {
        Status status = motor.getMotorSubmissionStatus();
        return status == Status.APPROVED || status == Status.WAITING_FOR_APPROVAL;
    }

    private boolean hasSubmittedMotor(List<MotorDTO> motors) {
        return motors != null && motors.stream()
                .filter(m -> m != null && m.getMotorSubmissionType() != null)
                .anyMatch(motor -> "SUBMIT".equalsIgnoreCase(motor.getMotorSubmissionType()));
    }

    private void validateMotorApprovalRequest(MotorApprovalRequest request) {
        if (request.getActionType() != Status.APPROVED
                && request.getActionType() != Status.REJECTED) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "actionType must be APPROVED or REJECTED.");
        }
        if (request.getActionType() == Status.REJECTED
                && (request.getRejectionReason() == null
                || request.getRejectionReason().isBlank())) {
            throw new AcemException(
                    ErrorCode.REJECTION_REASON_REQUIRED,
                    "rejectionReason is required for rejected motor.");
        }
    }

    private Batch getBatch(String batchId) {
        return batchRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId " + batchId));
    }

    private User getUser(String userId) {
        return userRepo.findByUserId(userId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"));
    }

    private void validateCurrentStage(Batch batch, Long subDepartmentId) {
        batchService.assertStageUnlockedAndActive(batch, subDepartmentId, "Trimming");
    }

    private int totalRequiredMotors(Batch batch) {
        return batch.getNumberOfMotors() != null
                ? batch.getNumberOfMotors()
                : batch.getMotorIds().size();
    }

    private void updateBatchStage(
            Batch batch,
            Long subDepartmentId,
            Status status,
            boolean submitted,
            String userId,
            String username,
            LocalDateTime now) {

        List<MotorStatusResponse> motorStatuses = buildFullMotorStatuses(
                batch,
                trimmingRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                        .map(Trimming::getMotors)
                        .orElse(Collections.emptyList()),
                batch.getMotorIds());
        batchService.syncStageProgress(
                batch.getBatchId(), subDepartmentId, status,
                null, null, motorStatuses,
                submitted ? PersonInfo.builder().id(userId).fullName(username).build() : null,
                now, false, null, null);
    }

    private void updateBatchAfterMotorApproval(
            Batch batch,
            Trimming form,
            Status batchStatus,
            String approverUserId,
            LocalDateTime now) {

        List<MotorStatusResponse> motorStatuses =
                buildFullMotorStatuses(batch, form.getMotors(), batch.getMotorIds());
        batchService.syncStageApproval(
                batch.getBatchId(), form.getSubDepartmentId(), batchStatus,
                null, null, motorStatuses,
                motorStatuses.stream().anyMatch(item ->
                        item.getMotorSubmissionStatus() == Status.APPROVED),
                form.getRemarks(), form.getRejectionReason(),
                approverUserId, now);
    }

    private record MotorCounts(
            int pending,
            int approved,
            int rejected,
            int inProgress) {
    }

    private List<DocumentDto> promoteTrimmingReportFiles(
            List<MotorDTO> motors,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        if (motors == null || motors.isEmpty()) {
            return newlyPromotedDocs;
        }

        for (MotorDTO motor : motors) {
            if (motor == null || motor.getSections() == null) {
                continue;
            }

            for (SectionDTO<?> section : motor.getSections()) {
                if (section instanceof TrimmingRemarksSectionDTO remarksSection) {
                    newlyPromotedDocs.addAll(promoteRemarksSectionFiles(
                            remarksSection, subDeptId, formId, existingFileIds, motor.getMotorId()));
                } else if (section instanceof TrimmingDrawingSectionDTO drawingSection) {
                    newlyPromotedDocs.addAll(promoteDrawingSectionFiles(
                            drawingSection, subDeptId, formId, existingFileIds, motor.getMotorId()));
                }
            }
        }
        return newlyPromotedDocs;
    }

    private List<DocumentDto> promoteRemarksSectionFiles(
            TrimmingRemarksSectionDTO remarksSection,
            Long deptId,
            String formId,
            Set<String> existingFileIds,
            String motorId) {

        List<DocumentDto> newlyPromotedList = new ArrayList<>();
        if (remarksSection == null || remarksSection.getSectionData() == null) {
            return newlyPromotedList;
        }

        for (TrimmingRemarksSectionDTO.TrimmingRemarksData data : remarksSection.getSectionData()) {
            if (data == null) {
                continue;
            }

            if (data.getReportFile() != null) {
                for (DocumentDto report : data.getReportFile()) {
                    promoteSingleFile(report, deptId, formId, existingFileIds, motorId, newlyPromotedList);
                }
            }
        }
        return newlyPromotedList;
    }

    private List<DocumentDto> promoteDrawingSectionFiles(
            TrimmingDrawingSectionDTO drawingSection,
            Long deptId,
            String formId,
            Set<String> existingFileIds,
            String motorId) {

        List<DocumentDto> newlyPromotedList = new ArrayList<>();
        if (drawingSection == null || drawingSection.getSectionData() == null) {
            return newlyPromotedList;
        }

        for (TrimmingDrawingSectionDTO.TrimmingDrawingData data : drawingSection.getSectionData()) {
            if (data == null || data.getDrawingFile() == null) {
                continue;
            }
            for (DocumentDto drawing : data.getDrawingFile()) {
                promoteSingleFile(drawing, deptId, formId, existingFileIds, motorId, newlyPromotedList);
            }
        }
        return newlyPromotedList;
    }

    private void promoteSingleFile(
            DocumentDto report,
            Long deptId,
            String formId,
            Set<String> existingFileIds,
            String motorId,
            List<DocumentDto> newlyPromotedList) {

        if (report == null || report.getFileId() == null || report.getFileId().isBlank()) {
            return;
        }

        String fileId = report.getFileId().trim();

        if (existingFileIds.contains(fileId)) {
            fileMetadataEnrichmentService.enrichDocument(report, deptId);
            return;
        }

        try {
            FileAttachments savedFile = fileBindingService.promoteAndBindFile(
                    fileId,
                    deptId,
                    formId,
                    FileBindingScope.forMotor(motorId)
            );
            report.setFileUrl(savedFile.getFilePath());
            report.setFileName(savedFile.getOriginalFileName());
            report.setStatus(savedFile.getStatus());
            report.setFileId(savedFile.getFileId());
            report.setMimeType(savedFile.getMimeType());
            report.setReadOnly(savedFile.isReadOnly());
            newlyPromotedList.add(report);

        } catch (Exception e) {
            log.error("File promotion failed for fileId: {}", fileId, e);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload failed, please try again."
            );
        }
    }

    private Set<String> extractMotorFileIds(MotorDTO motor) {
        return extractTrimmingFileIds(motor == null ? null : List.of(motor));
    }

    private void enrichTrimmingMotorFiles(List<MotorDTO> motors, Long subDeptId) {
        if (motors == null) {
            return;
        }
        for (MotorDTO motor : motors) {
            if (motor == null || motor.getSections() == null) {
                continue;
            }
            for (SectionDTO<?> section : motor.getSections()) {
                if (section instanceof TrimmingRemarksSectionDTO remarksSection
                        && remarksSection.getSectionData() != null) {
                    for (TrimmingRemarksSectionDTO.TrimmingRemarksData data : remarksSection.getSectionData()) {
                        if (data != null && data.getReportFile() != null) {
                            fileMetadataEnrichmentService.enrichDocuments(data.getReportFile(), subDeptId);
                        }
                    }
                } else if (section instanceof TrimmingDrawingSectionDTO drawingSection
                        && drawingSection.getSectionData() != null) {
                    for (TrimmingDrawingSectionDTO.TrimmingDrawingData data : drawingSection.getSectionData()) {
                        if (data != null && data.getDrawingFile() != null) {
                            fileMetadataEnrichmentService.enrichDocuments(data.getDrawingFile(), subDeptId);
                        }
                    }
                }
            }
        }
    }

    private void rollbackPromotedFiles(List<DocumentDto> promotedDocs) {
        if (promotedDocs == null || promotedDocs.isEmpty()) {
            return;
        }
        log.info("Starting rollback for {} promoted files...", promotedDocs.size());
        for (DocumentDto doc : promotedDocs) {
            if (doc != null && doc.getFileId() != null) {
                try {
                    fileBindingService.rollbackOrUnbindFile(doc.getFileId());
                } catch (Exception e) {
                    log.error("Failed to execute file rollback for fileId={}", doc.getFileId(), e);
                }
            }
        }
    }

    private Set<String> extractTrimmingFileIds(List<MotorDTO> motors) {
        Set<String> fileIds = new HashSet<>();
        if (motors == null || motors.isEmpty()) {
            return fileIds;
        }

        for (MotorDTO motor : motors) {
            if (motor == null || motor.getSections() == null) {
                continue;
            }

            for (SectionDTO<?> section : motor.getSections()) {
                if (section instanceof TrimmingRemarksSectionDTO remarksSection) {
                    if (remarksSection.getSectionData() != null) {
                        for (TrimmingRemarksSectionDTO.TrimmingRemarksData data : remarksSection.getSectionData()) {
                            if (data != null && data.getReportFile() != null) {
                                for (DocumentDto report : data.getReportFile()) {
                                    addFileIdIfValid(fileIds, report);
                                }
                            }
                        }
                    }
                } else if (section instanceof TrimmingDrawingSectionDTO drawingSection) {
                    if (drawingSection.getSectionData() != null) {
                        for (TrimmingDrawingSectionDTO.TrimmingDrawingData data : drawingSection.getSectionData()) {
                            if (data != null && data.getDrawingFile() != null) {
                                for (DocumentDto drawing : data.getDrawingFile()) {
                                    addFileIdIfValid(fileIds, drawing);
                                }
                            }
                        }
                    }
                }
            }
        }
        return fileIds;
    }

    private void addFileIdIfValid(Set<String> fileIds, DocumentDto report) {
        if (report != null && report.getFileId() != null && !report.getFileId().isBlank()) {
            fileIds.add(report.getFileId());
        }
    }

}
