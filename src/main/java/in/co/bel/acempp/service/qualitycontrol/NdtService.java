package in.co.bel.acempp.service.qualitycontrol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalRequest;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalResponse;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.ndt.*;
import in.co.bel.acempp.dto.ndt.motor.RadiographyObservationDto;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.ndt.motor.VisualInspectionDetailsDto;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.qualitycontrol.Ndt;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.FileSystemRepository;
import in.co.bel.acempp.repository.qualitycontrol.NdtRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NdtService {

    private final NdtRepository ndtRepo;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final BatchService batchService;
    private final FormIdGeneratorService formIdGenerator;
    private final ObjectMapper objectMapper;
    private final FileBindingService fileBindingService;
    private final FileSystemRepository fileSystemRepository;
    private final FileReadOnlyService fileReadOnlyService;
    private final MainBatchGateValidator mainBatchGateValidator;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;

    public NdtResponse createForm(CreateNdtRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Batch batch = getBatch(request.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());

        if (ndtRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "NDT form already submitted for this batch");
        }

        User creator = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        String formId = formIdGenerator.generate("ndts", "formId", "NDT", 3);

        // List to hold files promoted in this execution for potential rollback
        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            List<NdtMotorDto> motors = prepareIncomingMotors(request.getMotors(), batch);
            promotedDocs = promoteNdtFiles(motors, request.getSubDepartmentId(), formId, Collections.emptySet());
            Status batchStatus = calculateBatchStatus(motors, totalRequiredMotors(batch));

            Ndt entity = Ndt.builder()
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

            Ndt savedForm = ndtRepo.save(entity);
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
            NdtResponse.NdtResponseBuilder response = NdtResponse.builder()
                    .formId(savedForm.getFormId())
                    .batchId(savedForm.getBatchId())
                    .formStatus(batchStatus)
                    .motorCount(motors.size())
                    .pendingMotorCount(counts.pending())
                    .approvedMotorCount(counts.approved())
                    .rejectedMotorCount(counts.rejected())
                    .inProgressMotorCount(counts.inProgress())
                    .totalMotorCount(totalRequiredMotors(batch))
                    .motorStatuses(buildFullMotorStatuses(batch, motors, batch.getMotorIds()));
            User updater = getUser(userId);

            if (isDraft) {
                response.createdBy(PersonInfo.builder()
                                .id(updater.getUserId())
                                .fullName(updater.getUsername())
                                .build())
                        .createdOn(now);
            } else {
                response.submittedBy(PersonInfo.builder()
                        .id(userId)
                        .fullName(updater.getUsername())
                        .build())
                        .submittedOn(now);
            }

            log.info("NDT form {} created by {}", formId, userId);
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
                    "Failed to create NDT form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public NdtResponse updateForm(CreateNdtRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Ndt form = ndtRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "No NDT form found for formId: " + request.getFormId()));

        if (form.getStatus() == Status.APPROVED
                || form.getStatus() == Status.COMPLETELY_APPROVED) {
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
            Set<String> existingDbFileIds = extractNdtDtoFileIds(form.getMotors());

            // Promote only new temporary files
            promotedDocs = promoteNdtFiles(
                    request.getMotors(),
                    request.getSubDepartmentId(),
                    request.getFormId(),
                    existingDbFileIds
            );

            Map<String, NdtMotorDto> existingMotorMap = form.getMotors() == null
                    ? new LinkedHashMap<>()
                    : form.getMotors().stream().collect(Collectors.toMap(
                    NdtMotorDto::getMotorId,
                    Function.identity(),
                    (first, ignored) -> first,
                    LinkedHashMap::new));

            List<NdtMotorDto> incomingMotors = prepareIncomingMotors(request.getMotors(), batch);
            if (!isFinalFormSubmit) {
                for (NdtMotorDto incoming : incomingMotors) {
                    NdtMotorDto existing = existingMotorMap.get(incoming.getMotorId());
                    if (existing != null && isProtectedMotor(existing)) {
                        throw new AcemException(
                                ErrorCode.FORM_ALREADY_EXISTS,
                                "Motor " + incoming.getMotorId() + " cannot be modified.");
                    }
                }
            }

            for (NdtMotorDto incoming : incomingMotors) {
                NdtMotorDto existing = existingMotorMap.get(incoming.getMotorId());
                if (existing == null || !isProtectedMotor(existing)) {
                    // Retain the complete typed NDT motor and all nested inspection sections.
                    incoming.setRejectionReason(null);
                    existingMotorMap.put(incoming.getMotorId(), incoming);
                }
            }

            List<NdtMotorDto> updatedMotors = new ArrayList<>(existingMotorMap.values());
            Status batchStatus = calculateBatchStatus(updatedMotors, totalRequiredMotors(batch));

            User updater = getUser(userId);
            LocalDateTime now = LocalDateTime.now();
            form.setFormSubmissionType(request.getFormSubmissionType());
            form.setStatus(batchStatus);
            form.setSubDepartmentId(request.getSubDepartmentId());
            form.setMotors(updatedMotors);
            form.setRejectionReason(resolveFormRejectionReasonFromMotors(updatedMotors));
            form.setUpdatedBy(PersonInfo.builder()
                    .id(updater.getUserId())
                    .fullName(updater.getUsername())
                    .build());
            form.setUpdatedAt(now);

            Ndt savedForm = ndtRepo.save(form);
            updateBatchStage(
                    batch,
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    isFinalFormSubmit || hasSubmittedMotor(incomingMotors),
                    userId,
                    updater.getUsername(),
                    now);

            MotorCounts counts = countMotors(updatedMotors);
            NdtResponse.NdtResponseBuilder response =
                    NdtResponse.builder()
                            .formId(savedForm.getFormId())
                            .batchId(savedForm.getBatchId())
                            .formStatus(batchStatus)
                            .updatedBy(savedForm.getUpdatedBy())
                            .updatedOn(now)
                            .pendingMotorCount(counts.pending())
                            .approvedMotorCount(counts.approved())
                            .rejectedMotorCount(counts.rejected())
                            .inProgressMotorCount(counts.inProgress())
                            .totalMotorCount(totalRequiredMotors(batch))
                            .motorStatuses(buildFullMotorStatuses(
                                    batch,
                                    updatedMotors,
                                    batch.getMotorIds()));

            if (isFinalFormSubmit) {
                response.submittedBy(PersonInfo.builder()
                                .id(updater.getUserId())
                                .fullName(updater.getUsername())
                                .build())
                        .submittedOn(now);
            }

            log.info("NDT form {} updated by {}", savedForm.getFormId(), userId);
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
                    "Failed to update NDT form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public NdtResponse getFormDetails(String formId) {
        Ndt form = ndtRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "No NDT form found for formId: " + formId));
        Batch batch = getBatch(form.getBatchId());
        List<NdtMotorDto> motors = form.getMotors() != null
                ? form.getMotors()
                : Collections.emptyList();
        MotorCounts counts = countMotors(motors);
        int totalMotorCount = totalRequiredMotors(batch);
        enrichNdtMotorFiles(motors, form.getSubDepartmentId());

        return NdtResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .batchType(form.getBatchType())
                .formStatus(form.getStatus())
                .createdBy(batch.getCreatedBy() != null
                        ? batch.getCreatedBy()
                        : null)
                .createdAt(batch.getCreatedAt())
                .submittedBy(form.getCreatedBy())
                .submittedAt(form.getCreatedAt())
                .updatedBy(form.getUpdatedBy())
                .updatedAt(form.getUpdatedAt())
                .motors(motorDetails(motors))
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
    public MotorApprovalResponse changeStatusOfMotor(
            MotorApprovalRequest request,
            String approverUserId) {

        validateMotorApprovalRequest(request);
        Ndt form = ndtRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "NDT form not found"));

        if (request.getSubDepartmentId() != null
                && !request.getSubDepartmentId().equals(form.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The motor approval request does not belong to NDT");
        }
        if (form.getMotors() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "No motors are available in this form");
        }

        NdtMotorDto motor = form.getMotors().stream()
                .filter(item -> item.getMotorId().equals(request.getMotorId()))
                .findFirst()
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor not found"));
        if (motorStatus(motor) != Status.WAITING_FOR_APPROVAL) {
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

        User approver = getUser(approverUserId);
        LocalDateTime now = LocalDateTime.now();
        form.setUpdatedBy(PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build());
        form.setUpdatedAt(now);
        form.setRemarks(request.getRemarks());
        form.setRejectionReason(resolveFormRejectionReasonFromMotors(form.getMotors()));

        Batch batch = getBatch(form.getBatchId());
        int totalMotorCount = totalRequiredMotors(batch);
        Status batchStatus = calculateBatchStatus(form.getMotors(), totalMotorCount);
        form.setStatus(batchStatus);
        if (batchStatus == Status.COMPLETELY_APPROVED) {
            form.setFormSubmissionType(Status.SUBMIT);
            form.setRejectionReason(null);
        }
        ndtRepo.save(form);
        updateBatchAfterMotorApproval(batch, form, batchStatus, approver, now);
        if (request.getActionType() == Status.APPROVED) {
            fileReadOnlyService.markReadOnly(
                    form.getSubDepartmentId(), extractMotorFileIds(motor));
            try {
                Map<String, Object> blockchainPayload = buildMotorBlockchainPayload(form, motor, approver.getUsername(), now);
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

        MotorCounts counts = countMotors(form.getMotors());
        return MotorApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .motorId(request.getMotorId())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .motorSubmissionStatus(motorStatus(motor))
                .batchStatus(batchStatus)
                .allMotorsApproved(counts.approved() == totalMotorCount)
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .totalMotorCount(totalMotorCount)
                .remarks(form.getRemarks())
                .rejectionReason(form.getRejectionReason())
                .actionBy(approver.getUsername())
                .actionAt(now)
                .build();
    }

    private Map<String, Object> buildMotorBlockchainPayload(Ndt form, NdtMotorDto motor, String approverName, LocalDateTime approvalTime) {
        Map<String, Object> rootPayload = new LinkedHashMap<>();

        // 1. Audit / Metadata Object
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getCreatedBy() != null) {
                auditMap.put("createdBy", form.getCreatedBy().getFullName());
            }
            if (form.getCreatedAt() != null) {
                auditMap.put("createdAt", form.getCreatedAt());
            }
        }

        if (approverName != null) {
            auditMap.put("approvedBy", approverName);
            auditMap.put("approvedAt", approvalTime);
        } else if (form != null && form.getUpdatedBy() != null) {
            auditMap.put("approvedBy", form.getUpdatedBy().getFullName());
            auditMap.put("approvedAt", form.getUpdatedAt());
        }
        rootPayload.put("audit", auditMap);

        // 2. Motor Details Object
        Map<String, Object> motorMap = new LinkedHashMap<>();
        if (motor != null) {
            motorMap.put("motorId", motor.getMotorId());
            motorMap.put("radiographyDetails", motor.getRadiographyDetails() == null ? Collections.emptyMap() : motor.getRadiographyDetails());
            motorMap.put("additionalExposureDetails", motor.getAdditionalExposureDetails() == null ? Collections.emptyList() : motor.getAdditionalExposureDetails());
            motorMap.put("radiographyObservations", motor.getRadiographyObservations() == null ? Collections.emptyList() : motor.getRadiographyObservations());
            motorMap.put("visualInspectionDetails", motor.getVisualInspectionDetails() == null ? Collections.emptyList() : motor.getVisualInspectionDetails());

            List<String> videoHashes = Collections.emptyList();
            if (motor.getUploadedVideos() != null && !motor.getUploadedVideos().isEmpty()) {
                videoHashes = motor.getUploadedVideos().stream()
                        .map(DocumentDto::getFileId)
                        .map(this::getFileHash)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            motorMap.put("uploadedVideos", videoHashes);

            if (motor.getSignedNdtReport() != null) {
                var signedReport = motor.getSignedNdtReport();
                Map<String, Object> reportMap = new LinkedHashMap<>();
                reportMap.put("additionalRemarks", signedReport.getAdditionalRemarks());

                String reportHash = null;
                if (signedReport.getReport() != null && signedReport.getReport().getFileId() != null) {
                    reportHash = getFileHash(signedReport.getReport().getFileId());
                }
                reportMap.put("report", reportHash);

                motorMap.put("signedNdtReport", reportMap);
            } else {
                motorMap.put("signedNdtReport", Collections.emptyMap());
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

    public ApproverStatusResponse changeStatus(
            ApproverStatusRequest request,
            String approverUserId) {

        Ndt entity = ndtRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "NDT form not found: " + request.getFormId()));
        User approver = getUser(approverUserId);

        ApprovalStatusUtil.requireLegacyFinalApprovalState(entity.getStatus());
        ApprovalStatusUtil.applyStatusChange(
                request.getActionType(),
                request.getSubDepartmentId(),
                request.getRemarks(),
                request.getRejectionReason(),
                approver.getUsername(),
                entity::setStatus,
                entity::setRemarks,
                entity::setRejectionReason,
                entity::setUpdatedBy,
                entity::setUpdatedAt);
        entity.setUpdatedBy(PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build());

        ndtRepo.save(entity);
        batchService.updateBatchStatus(
                entity.getBatchId(),
                entity.getSubDepartmentId(),
                entity.getStatus(),
                entity.getRemarks(),
                entity.getRejectionReason(),
                approverUserId);
        if (request.getActionType() == Status.APPROVED
                || request.getActionType() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(entity.getFormId(), entity.getSubDepartmentId());
        }

        return ApproverStatusResponse.builder()
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionAt(entity.getUpdatedAt())
                .actionBy(entity.getUpdatedBy())
                .build();
    }

    private List<NdtMotorDto> prepareIncomingMotors(
            List<NdtMotorDto> motorForms,
            Batch batch) {

        if (motorForms == null || motorForms.isEmpty()) {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one motor is required");
        }
        if (batch.getMotorIds() == null || batch.getMotorIds().isEmpty()) {
            throw new AcemException(
                    ErrorCode.MOTORS_NOT_FOUND,
                    "Batch has no associated motors");
        }

        Map<String, NdtMotorDto> motorsById = new LinkedHashMap<>();
        for (NdtMotorDto motorForm : motorForms) {
            if (motorForm == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Each motor form is required");
            }

            String rawMotorId = motorForm.getMotorId();
            if (rawMotorId == null || rawMotorId.isBlank()) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Each motor must have a motorId");
            }
            if (!batch.getMotorIds().contains(rawMotorId)) {
                throw new AcemException(
                        ErrorCode.MOTORS_NOT_FOUND,
                        "Motor " + rawMotorId + " is not part of this batch");
            }
            if (motorsById.containsKey(rawMotorId)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor " + rawMotorId + " is duplicated in the request");
            }
            mainBatchGateValidator.assertMotorUnlocked(
                    batch, MainBatchStageConstants.NDT, rawMotorId);

            Status submissionType = motorForm.getMotorSubmissionType();
            if (submissionType != Status.DRAFT && submissionType != Status.SUBMIT) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorSubmissionType must be DRAFT or SUBMIT for motor " + rawMotorId);
            }

            motorForm.setMotorSubmissionType(submissionType);
            motorForm.setMotorSubmissionStatus(
                    submissionType == Status.DRAFT
                            ? Status.IN_PROGRESS
                            : Status.WAITING_FOR_APPROVAL);
            motorForm.setRejectionReason(null);
            motorsById.put(rawMotorId, motorForm);
        }
        return new ArrayList<>(motorsById.values());
    }

    private static String resolveFormRejectionReasonFromMotors(List<NdtMotorDto> motors) {
        if (motors == null) {
            return null;
        }
        return motors.stream()
                .filter(m -> m != null && m.getMotorSubmissionStatus() == Status.REJECTED)
                .map(NdtMotorDto::getRejectionReason)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<NdtMotorDto> motorDetails(List<NdtMotorDto> motors) {
        if (motors == null) {
            return Collections.emptyList();
        }
        return motors.stream()
                .collect(Collectors.toList());
    }

    private List<MotorStatusResponse> buildFullMotorStatuses(
            Batch batch,
            List<NdtMotorDto> savedMotors,
            List<String> batchMotorIds) {

        Map<String, NdtMotorDto> savedMap = savedMotors == null
                ? Collections.emptyMap()
                : savedMotors.stream().collect(Collectors.toMap(
                NdtMotorDto::getMotorId,
                Function.identity(),
                (first, ignored) -> first));

        List<MotorStatusResponse> statuses = new ArrayList<>();
        for (String motorId : batchMotorIds) {
            NdtMotorDto motor = savedMap.get(motorId);
            statuses.add(motor == null
                    ? MotorStatusResponse.builder()
                    .motorId(motorId)
                    .motorSubmissionType(Status.DRAFT)
                    .motorSubmissionStatus(MainBatchStageProgressHelper.defaultMotorSubmissionStatus(
                            batch, MainBatchStageConstants.NDT, motorId))
                    .build()
                    : MotorStatusResponse.builder()
                    .motorId(motorId)
                    .motorSubmissionType(motorSubmissionType(motor))
                    .motorSubmissionStatus(motorStatus(motor))
                    .rejectionReason(motor.getRejectionReason())
                    .build());
        }
        return BatchMotorDiscardHelper.overlayMotorStatuses(batch, statuses);
    }

    private Status calculateBatchStatus(
            List<NdtMotorDto> savedMotors,
            int totalRequiredMotors) {

        if (savedMotors == null || savedMotors.isEmpty()) {
            return Status.TO_BE_INITIATED;
        }
        if (savedMotors.stream().anyMatch(m -> motorStatus(m) == Status.REJECTED)) {
            return Status.REJECTED;
        }

        long waitingCount = savedMotors.stream()
                .filter(m -> motorStatus(m) == Status.WAITING_FOR_APPROVAL)
                .count();
        long approvedCount = savedMotors.stream()
                .filter(m -> motorStatus(m) == Status.APPROVED)
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
        if (approvedCount > 0) return Status.IN_PROGRESS;
        boolean anyStarted = savedMotors.stream()
                .anyMatch(m -> motorStatus(m) == Status.IN_PROGRESS
                        || motorSubmissionType(m) == Status.DRAFT);
        return anyStarted ? Status.IN_PROGRESS : Status.TO_BE_INITIATED;
    }

    private MotorCounts countMotors(List<NdtMotorDto> motors) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int inProgress = 0;
        for (NdtMotorDto motor : motors) {
            Status status = motorStatus(motor);
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

    private boolean isProtectedMotor(NdtMotorDto motor) {
        Status status = motorStatus(motor);
        return status == Status.APPROVED || status == Status.WAITING_FOR_APPROVAL;
    }

    private Status motorSubmissionType(NdtMotorDto motor) {
        return motor == null ? null : motor.getMotorSubmissionType();
    }

    private boolean hasSubmittedMotor(List<NdtMotorDto> motors) {
        return motors != null && motors.stream()
                .filter(motor -> motor != null)
                .anyMatch(motor -> motorSubmissionType(motor) == Status.SUBMIT);
    }

    private Status motorStatus(NdtMotorDto motor) {
        return motor == null ? null : motor.getMotorSubmissionStatus();
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
        batchService.assertStageUnlockedAndActive(batch, subDepartmentId, "NDT");
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
                ndtRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                        .map(Ndt::getMotors)
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
            Ndt form,
            Status batchStatus,
            User approver,
            LocalDateTime now) {

        List<MotorStatusResponse> motorStatuses =
                buildFullMotorStatuses(batch, form.getMotors(), batch.getMotorIds());
        batchService.syncStageApproval(
                batch.getBatchId(), form.getSubDepartmentId(), batchStatus,
                null, null, motorStatuses,
                motorStatuses.stream().anyMatch(item ->
                        item.getMotorSubmissionStatus() == Status.APPROVED),
                form.getRemarks(), form.getRejectionReason(),
                approver.getUserId(), now);
    }

    private record MotorCounts(
            int pending,
            int approved,
            int rejected,
            int inProgress) {
    }

    private Set<String> extractNdtDtoFileIds(List<NdtMotorDto> motors) {
        Set<String> fileIds = new HashSet<>();
        if (motors == null || motors.isEmpty()) {
            return fileIds;
        }

        for (NdtMotorDto motor : motors) {
            if (motor == null) {
                continue;
            }

            // 1. Uploaded videos
            extractFileIdsFromDocs(motor.getUploadedVideos(), fileIds);

            // 2. Signed NDT report
            if (motor.getSignedNdtReport() != null) {
                extractFileIdFromDoc(motor.getSignedNdtReport().getReport(), fileIds);
            }

            // 3. Radiography observation images
            if (motor.getRadiographyObservations() != null) {
                for (RadiographyObservationDto observation : motor.getRadiographyObservations()) {
                    if (observation != null) {
                        extractFileIdsFromDocs(observation.getUploadedImages(), fileIds);
                    }
                }
            }

            // 4. ADDED: Visual inspection images
            if (motor.getVisualInspectionDetails() != null) {
                for (VisualInspectionDetailsDto visualDetail : motor.getVisualInspectionDetails()) {
                    if (visualDetail != null) {
                        extractFileIdsFromDocs(visualDetail.getUploadedImages(), fileIds);
                    }
                }
            }
        }
        return fileIds;
    }

    private void extractFileIdsFromDocs(List<DocumentDto> docs, Set<String> targetSet) {
        if (docs != null) {
            for (DocumentDto doc : docs) {
                extractFileIdFromDoc(doc, targetSet);
            }
        }
    }

    private void extractFileIdFromDoc(DocumentDto doc, Set<String> targetSet) {
        if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
            targetSet.add(doc.getFileId());
        }
    }

    private List<DocumentDto> promoteNdtFiles(
            List<NdtMotorDto> motors,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        if (motors == null || motors.isEmpty()) {
            return newlyPromotedDocs;
        }

        for (NdtMotorDto motor : motors) {
            if (motor == null) {
                continue;
            }

            // 1. Promote uploaded videos
            String motorId = motor.getMotorId();
            newlyPromotedDocs.addAll(promoteDocumentList(
                    motor.getUploadedVideos(), subDeptId, formId, existingFileIds, motorId));

            // 2. Promote signed NDT report
            if (motor.getSignedNdtReport() != null) {
                DocumentDto doc = promoteDocument(
                        motor.getSignedNdtReport().getReport(), subDeptId, formId, existingFileIds, motorId);
                if (doc != null) {
                    newlyPromotedDocs.add(doc);
                }
            }

            // 3. Promote radiography observation images
            if (motor.getRadiographyObservations() != null) {
                for (RadiographyObservationDto observation : motor.getRadiographyObservations()) {
                    if (observation != null) {
                        newlyPromotedDocs.addAll(promoteDocumentList(
                                observation.getUploadedImages(), subDeptId, formId, existingFileIds, motorId));
                    }
                }
            }

            // 4. ADDED: Promote visual inspection images
            if (motor.getVisualInspectionDetails() != null) {
                for (VisualInspectionDetailsDto visualDetail : motor.getVisualInspectionDetails()) {
                    if (visualDetail != null) {
                        newlyPromotedDocs.addAll(promoteDocumentList(
                                visualDetail.getUploadedImages(), subDeptId, formId, existingFileIds, motorId));
                    }
                }
            }
        }
        return newlyPromotedDocs;
    }

    private List<DocumentDto> promoteDocumentList(
            List<DocumentDto> documents,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds,
            String motorId) {

        List<DocumentDto> newlyPromotedList = new ArrayList<>();
        if (documents != null && !documents.isEmpty()) {
            for (DocumentDto doc : documents) {
                DocumentDto newlyPromotedDoc = promoteDocument(doc, subDeptId, formId, existingFileIds, motorId);
                if (newlyPromotedDoc != null) {
                    newlyPromotedList.add(newlyPromotedDoc);
                }
            }
        }
        return newlyPromotedList;
    }

    private DocumentDto promoteDocument(
            DocumentDto doc,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds,
            String motorId) {

        if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
            String fileId = doc.getFileId().trim();

            // SCENARIO 1: File is already bound -> Enrich metadata from DB without re-promoting
            if (existingFileIds.contains(fileId)) {
                fileMetadataEnrichmentService.enrichDocument(doc, subDeptId);
                return null; // Exclude from newlyPromotedDocs to protect existing attachments during rollback
            }

            // SCENARIO 2: New Temp File -> Promote & Bind to form
            try {
                FileAttachments savedFile = fileBindingService.promoteAndBindFile(
                        fileId,
                        subDeptId,
                        formId,
                        FileBindingScope.forMotor(motorId)
                );
                doc.setFileUrl(savedFile.getFilePath());
                doc.setFileName(savedFile.getOriginalFileName());
                doc.setStatus(savedFile.getStatus());
                doc.setFileId(savedFile.getFileId());
                doc.setMimeType(savedFile.getMimeType());
                doc.setReadOnly(savedFile.isReadOnly());
                return doc; // Return enriched doc for tracking in rollback list

            } catch (Exception e) {
                log.error("File promotion failed for fileId: {}", fileId, e);
                throw new AcemException(
                        ErrorCode.FILE_TRANSFER_ERROR,
                        "File upload failed, please try again."
                );
            }
        }
        return null;
    }

    private Set<String> extractMotorFileIds(NdtMotorDto motor) {
        Set<String> fileIds = new HashSet<>();
        if (motor == null) {
            return fileIds;
        }
        extractFileIdsFromDocs(motor.getUploadedVideos(), fileIds);
        if (motor.getSignedNdtReport() != null) {
            extractFileIdFromDoc(motor.getSignedNdtReport().getReport(), fileIds);
        }
        if (motor.getRadiographyObservations() != null) {
            for (RadiographyObservationDto observation : motor.getRadiographyObservations()) {
                if (observation != null) {
                    extractFileIdsFromDocs(observation.getUploadedImages(), fileIds);
                }
            }
        }
        if (motor.getVisualInspectionDetails() != null) {
            for (VisualInspectionDetailsDto visualDetail : motor.getVisualInspectionDetails()) {
                if (visualDetail != null) {
                    extractFileIdsFromDocs(visualDetail.getUploadedImages(), fileIds);
                }
            }
        }
        return fileIds;
    }

    private void enrichNdtMotorFiles(List<NdtMotorDto> motors, Long subDepartmentId) {
        if (motors == null) {
            return;
        }
        for (NdtMotorDto motor : motors) {
            if (motor == null) {
                continue;
            }
            fileMetadataEnrichmentService.enrichDocuments(motor.getUploadedVideos(), subDepartmentId);
            if (motor.getSignedNdtReport() != null) {
                fileMetadataEnrichmentService.enrichDocument(
                        motor.getSignedNdtReport().getReport(), subDepartmentId);
            }
            if (motor.getRadiographyObservations() != null) {
                for (RadiographyObservationDto observation : motor.getRadiographyObservations()) {
                    if (observation != null) {
                        fileMetadataEnrichmentService.enrichDocuments(
                                observation.getUploadedImages(), subDepartmentId);
                    }
                }
            }
            if (motor.getVisualInspectionDetails() != null) {
                for (VisualInspectionDetailsDto visualDetail : motor.getVisualInspectionDetails()) {
                    if (visualDetail != null) {
                        fileMetadataEnrichmentService.enrichDocuments(
                                visualDetail.getUploadedImages(), subDepartmentId);
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
}
