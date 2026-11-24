package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalRequest;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalResponse;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.postcureoperations.*;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.manufacturing.PostCureOperations;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.PostCureInhibitorType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.manufacturing.PostCureRepository;
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
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import in.co.bel.acempp.util.FileIdExtractionUtil;
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
public class PostCureOptService {

    private final PostCureRepository postCureRepo;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final MaterialRepository materialRepository;
    private final BatchService batchService;
    private final FormIdGeneratorService formIdGenerator;
    private final PostCureDtoMapper postCureDtoMapper;
    private final FileBindingService fileBindingService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;
    private final MainBatchGateValidator mainBatchGateValidator;

    public PostCureResponse createForm(PostCureRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Batch batch = getBatch(request.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());

        if (postCureRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Post cure form already submitted for this batch");
        }

        String formId = formIdGenerator.generate(
                "post_cure_operations",
                "formId",
                "PCF",
                3);

        // Promote attachment files from temporary to permanent disk storage
        List<DocumentDto> promotedDocs = new ArrayList<>();
        try {
            promotedDocs = promotePostCureFiles(
                    request.getMotors(),
                    request.getSubDepartmentId(),
                    formId,
                    Collections.emptySet()
            );
        } catch (Exception e) {
            log.error("File promotion failed during Post Cure form creation", e);
            rollbackPromotedFiles(promotedDocs);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload failed, please try again."
            );
        }

        try {
            List<PostCureMotor> motors = prepareIncomingMotors(request.getMotors(), batch);
            Status batchStatus = calculateBatchStatus(motors, totalRequiredMotors(batch));
            User creator = getUser(userId);
            LocalDateTime now = LocalDateTime.now();

            PostCureOperations entity = PostCureOperations.builder()
                    .formId(formId)
                    .batchId(request.getBatchId())
                    .subDepartmentId(request.getSubDepartmentId())
                    .formSubmissionType(request.getFormSubmissionType())
                    .status(batchStatus)
                    .motors(motors)
                    .submittedBy(PersonInfo.builder()
                            .id(userId)
                            .fullName(creator.getUsername())
                            .build())
                    .submittedAt(now)
                    .build();

            PostCureOperations savedForm = postCureRepo.save(entity);
            updateBatchStage(
                    batch,
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    request.getFormSubmissionType() == Status.SUBMIT || hasSubmittedMotor(motors),
                    userId,
                    creator.getUsername(),
                    now);

            MotorCounts counts = countMotors(motors);
            log.info("Post Cure form {} created by {}", formId, userId);

            return PostCureResponse.builder()
                    .formId(savedForm.getFormId())
                    .batchId(savedForm.getBatchId())
                    .batchType(batch.getBatchType())
                    .status(batchStatus)
                    .submittedBy(PersonInfo.builder()
                            .id(userId)
                            .fullName(creator.getUsername())
                            .build())
                    .submittedAt(now)
                    .pendingMotorCount(counts.pending())
                    .approvedMotorCount(counts.approved())
                    .rejectedMotorCount(counts.rejected())
                    .inProgressMotorCount(counts.inProgress())
                    .totalMotorCount(totalRequiredMotors(batch))
                    .motorStatuses(buildFullMotorStatuses(batch, motors, batch.getMotorIds()))
                    .build();

        } catch (Exception e) {
            log.error("Form creation failed after file promotion, rolling back files for formId {}", formId, e);
            rollbackPromotedFiles(promotedDocs);
            throw e;
        }
    }

    public PostCureResponse updateForm(
            PostCureRequest request,
            String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());

        PostCureOperations form = postCureRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "No Post Cure form exists for formId: " + request.getFormId()));

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

        Set<String> existingFileIds = extractExistingFormFileIds(form);

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        try {
            newlyPromotedDocs = promotePostCureFiles(
                    request.getMotors(),
                    request.getSubDepartmentId(),
                    form.getFormId(),
                    existingFileIds
            );
        } catch (Exception e) {
            log.error("File promotion failed during Post Cure form update for formId {}", form.getFormId(), e);
            rollbackPromotedFiles(newlyPromotedDocs);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload failed, please try again."
            );
        }

        try {
            boolean isFinalFormSubmit = request.getFormSubmissionType() == Status.SUBMIT;

            Map<String, PostCureMotor> existingMotorMap = form.getMotors() == null
                    ? new LinkedHashMap<>()
                    : form.getMotors().stream().collect(Collectors.toMap(
                    PostCureMotor::getMotorId,
                    Function.identity(),
                    (first, ignored) -> first,
                    LinkedHashMap::new));

            List<PostCureMotor> incomingMotors = prepareIncomingMotors(request.getMotors(), batch);
            if (!isFinalFormSubmit) {
                for (PostCureMotor incoming : incomingMotors) {
                    PostCureMotor existing = existingMotorMap.get(incoming.getMotorId());
                    if (existing != null && isProtectedMotor(existing)) {
                        throw new AcemException(
                                ErrorCode.FORM_ALREADY_EXISTS,
                                "Motor " + incoming.getMotorId() + " cannot be modified.");
                    }
                }
            }

            for (PostCureMotor incoming : incomingMotors) {
                PostCureMotor existing = existingMotorMap.get(incoming.getMotorId());
                if (existing == null || !isProtectedMotor(existing)) {
                    incoming.setRejectionReason(null);
                    existingMotorMap.put(incoming.getMotorId(), incoming);
                }
            }

            List<PostCureMotor> updatedMotors = new ArrayList<>(existingMotorMap.values());
            Status batchStatus = calculateBatchStatus(updatedMotors, totalRequiredMotors(batch));

            User updater = getUser(userId);
            LocalDateTime now = LocalDateTime.now();
            form.setFormSubmissionType(request.getFormSubmissionType());
            form.setStatus(batchStatus);
            form.setSubDepartmentId(request.getSubDepartmentId());
            form.setMotors(updatedMotors);
            form.setRejectionReason(resolveFormRejectionReasonFromMotors(updatedMotors));
            form.setUpdatedBy(PersonInfo.builder()
                    .id(userId)
                    .fullName(updater.getUsername())
                    .build());
            form.setUpdatedAt(now);
            if (isFinalFormSubmit) {
                form.setSubmittedBy(PersonInfo.builder()
                        .id(userId)
                        .fullName(updater.getUsername())
                        .build());
                form.setSubmittedAt(now);
            }

            PostCureOperations savedForm = postCureRepo.save(form);
            updateBatchStage(
                    batch,
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    isFinalFormSubmit || hasSubmittedMotor(incomingMotors),
                    userId,
                    updater.getUsername(),
                    now);

            MotorCounts counts = countMotors(updatedMotors);
            log.info("Post Cure form {} updated by {}", request.getFormId(), userId);

            return PostCureResponse.builder()
                    .formId(savedForm.getFormId())
                    .batchId(savedForm.getBatchId())
                    .batchType(batch.getBatchType())
                    .status(batchStatus)
                    .updatedBy(userId)
                    .updatedAt(now)
                    .pendingMotorCount(counts.pending())
                    .approvedMotorCount(counts.approved())
                    .rejectedMotorCount(counts.rejected())
                    .inProgressMotorCount(counts.inProgress())
                    .totalMotorCount(totalRequiredMotors(batch))
                    .motorStatuses(buildFullMotorStatuses(batch, updatedMotors, batch.getMotorIds()))
                    .build();

        } catch (Exception e) {
            log.error("Form update failed after file promotion, rolling back newly promoted files for formId {}", form.getFormId(), e);
            rollbackPromotedFiles(newlyPromotedDocs);
            throw e;
        }
    }

    public PostCureResponse getFormDetails(String formId) {
        PostCureOperations form = postCureRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "No Post Cure form found for formId: " + formId));
        Batch batch = getBatch(form.getBatchId());
        List<PostCureMotor> motors = form.getMotors() != null
                ? form.getMotors()
                : Collections.emptyList();
        enrichPostCureMotorFiles(motors, form.getSubDepartmentId());
        MotorCounts counts = countMotors(motors);
        int totalMotorCount = totalRequiredMotors(batch);

        return PostCureResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .batchType(batch.getBatchType())
                .subDepartmentId(form.getSubDepartmentId())
                .status(form.getStatus())
                .motors(motors)
                .allMotorsApproved(counts.approved() == totalMotorCount)
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .inProgressMotorCount(counts.inProgress())
                .totalMotorCount(totalMotorCount)
                .motorStatuses(buildFullMotorStatuses(batch, motors, batch.getMotorIds()))
                .submittedBy(form.getSubmittedBy())
                .submittedAt(form.getSubmittedAt())
                .createdBy(batch.getCreatedBy() != null
                        ? batch.getCreatedBy().getId()
                        : null)
                .createdAt(batch.getCreatedAt())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public MotorApprovalResponse changeStatusOfMotor(
            MotorApprovalRequest request,
            String approverUserId) {

        validateMotorApprovalRequest(request);
        PostCureOperations form = postCureRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Post cure form not found"));

        if (request.getSubDepartmentId() != null
                && !request.getSubDepartmentId().equals(form.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The motor approval request does not belong to post cure");
        }
        if (form.getMotors() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "No motors are available in this form");
        }

        PostCureMotor motor = form.getMotors().stream()
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
            form.setFormSubmissionType(Status.SUBMIT);
            form.setSubmittedBy(form.getUpdatedBy());
            form.setSubmittedAt(now);
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

        postCureRepo.save(form);
        updateBatchAfterMotorApproval(
                batch,
                form,
                batchStatus,
                approverUserId,
                now);

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
                .actionBy(approverUserId)
                .actionAt(now)
                .build();
    }

    private String getFileHash(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return fileBindingService.getFileHash(fileId);
    }

    private Map<String, Object> buildMotorBlockchainPayload(
            PostCureOperations form,
            PostCureMotor motor,
            String approverUserId,
            LocalDateTime approvalTime) {

        Map<String, Object> rootPayload = new LinkedHashMap<>();

        // 1. Audit / Metadata Object (Separated created & approved info)
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getSubmittedBy() != null) {
                auditMap.put("createdBy", form.getSubmittedBy().getFullName());
            }
            if (form.getSubmittedAt() != null) {
                auditMap.put("createdAt", form.getSubmittedAt());
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

        // 2. Motor Technical Details Object (Separated operations)
        Map<String, Object> motorMap = new LinkedHashMap<>();
        if (motor != null) {
            motorMap.put("motorId", motor.getMotorId());
            motorMap.put("inhibitorType", motor.getInhibitorType());

            // 1. Loose Flap Filling Details
            if (motor.getLooseFlapFillingDetails() != null) {
                var looseFlap = motor.getLooseFlapFillingDetails();
                Map<String, Object> looseFlapMap = new LinkedHashMap<>();
                looseFlapMap.put("bellowRemovalDetails", looseFlap.getBellowRemovalDetails() == null ? Collections.emptyList() : looseFlap.getBellowRemovalDetails());
                looseFlapMap.put("epoxyPreparationIngredients", looseFlap.getEpoxyPreparationIngredients());

                if (looseFlap.getQualificationDetails() != null) {
                    looseFlapMap.put("qualificationDetails", mapQualificationDetails(looseFlap.getQualificationDetails()));
                }

                looseFlapMap.put("fillingDetails", looseFlap.getFillingDetails() == null ? Collections.emptyList() : looseFlap.getFillingDetails());
                motorMap.put("looseFlapFillingDetails", looseFlapMap);
            }

            // 2. Inhibition Details
            if (motor.getInhibitionDetails() != null) {
                var inhibition = motor.getInhibitionDetails();
                Map<String, Object> inhibitionMap = new LinkedHashMap<>();
                inhibitionMap.put("inhibitorType", inhibition.getInhibitorType());
                inhibitionMap.put("premixDetails", inhibition.getPremixDetails());
                inhibitionMap.put("finalMixDetails", inhibition.getFinalMixDetails());

                if (inhibition.getQualificationDetails() != null) {
                    inhibitionMap.put("qualificationDetails", mapQualificationDetails(inhibition.getQualificationDetails()));
                }

                inhibitionMap.put("inhibitorBatchDetails", inhibition.getInhibitorBatchDetails());
                inhibitionMap.put("applicationDetails", inhibition.getApplicationDetails() == null ? Collections.emptyList() : inhibition.getApplicationDetails());
                inhibitionMap.put("dispatchDetails", inhibition.getDispatchDetails());
                motorMap.put("inhibitionDetails", inhibitionMap);
            }
        }
        rootPayload.put("motorDetails", motorMap);

        return rootPayload;
    }

    /**
     * Helper to map QualificationDetails and convert DocumentDto entries into file hash strings.
     */
    private Map<String, Object> mapQualificationDetails(PostCureSectionPayloads.QualificationDetails qual) {
        Map<String, Object> qualMap = new LinkedHashMap<>();
        qualMap.put("batchNo", qual.getBatchNo());
        qualMap.put("preparationDateTime", qual.getPreparationDateTime());
        qualMap.put("parameters", qual.getParameters());

        // Convert DocumentDto list to file hashes list
        List<String> qcReportHashes = Collections.emptyList();
        if (qual.getQcReport() != null && !qual.getQcReport().isEmpty()) {
            qcReportHashes = qual.getQcReport().stream()
                    .map(DocumentDto::getFileId) // Assuming DocumentDto has getFileId()
                    .map(this::getFileHash)      // Reusing your file hash retrieval function
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        qualMap.put("qcReport", qcReportHashes);

        return qualMap;
    }

    public ApproverStatusResponse changeStatus(
            ApproverStatusRequest request,
            String approverName) {

        PostCureOperations entity = postCureRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Post cure form not found: " + request.getFormId()));

        ApprovalStatusUtil.requireLegacyFinalApprovalState(entity.getStatus());
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

        postCureRepo.save(entity);
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

    private List<PostCureMotor> prepareIncomingMotors(
            List<PostCureMotor> motorForms,
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

        Map<String, PostCureMotor> motorsById = new LinkedHashMap<>();
        for (PostCureMotor motorForm : motorForms) {
            if (motorForm == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Each motor form is required");
            }

            String motorId = motorForm.getMotorId();
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
                    batch, MainBatchStageConstants.POST_CURE, motorId);

            Status submissionType = motorForm.getMotorSubmissionType();
            if (submissionType != Status.DRAFT && submissionType != Status.SUBMIT) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorSubmissionType must be DRAFT or SUBMIT for motor " + motorId);
            }

            validateMotorPayload(motorForm);
            PostCureMotor completeMotorForm = postCureDtoMapper.toPersistence(motorForm);
            completeMotorForm.setMotorSubmissionStatus(
                    submissionType == Status.DRAFT
                            ? Status.IN_PROGRESS
                            : Status.WAITING_FOR_APPROVAL);
            completeMotorForm.setRejectionReason(null);
            motorsById.put(motorId, completeMotorForm);
        }
        return new ArrayList<>(motorsById.values());
    }

    private static String resolveFormRejectionReasonFromMotors(List<PostCureMotor> motors) {
        if (motors == null) {
            return null;
        }
        return motors.stream()
                .filter(m -> m != null && m.getMotorSubmissionStatus() == Status.REJECTED)
                .map(PostCureMotor::getRejectionReason)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<MotorStatusResponse> buildFullMotorStatuses(
            Batch batch,
            List<PostCureMotor> savedMotors,
            List<String> batchMotorIds) {

        Map<String, PostCureMotor> savedMap = savedMotors == null
                ? Collections.emptyMap()
                : savedMotors.stream().collect(Collectors.toMap(
                PostCureMotor::getMotorId,
                Function.identity(),
                (first, ignored) -> first));

        List<MotorStatusResponse> statuses = new ArrayList<>();
        for (String motorId : batchMotorIds) {
            PostCureMotor motor = savedMap.get(motorId);
            statuses.add(motor == null
                    ? MotorStatusResponse.builder()
                    .motorId(motorId)
                    .motorSubmissionType(Status.DRAFT)
                    .motorSubmissionStatus(MainBatchStageProgressHelper.defaultMotorSubmissionStatus(
                            batch, MainBatchStageConstants.POST_CURE, motorId))
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
            List<PostCureMotor> savedMotors,
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

    private MotorCounts countMotors(List<PostCureMotor> motors) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int inProgress = 0;
        for (PostCureMotor motor : motors) {
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

    private boolean isProtectedMotor(PostCureMotor motor) {
        Status status = motorStatus(motor);
        return status == Status.APPROVED || status == Status.WAITING_FOR_APPROVAL;
    }

    private Status motorSubmissionType(PostCureMotor motor) {
        return motor.getMotorSubmissionType();
    }

    private boolean hasSubmittedMotor(List<PostCureMotor> motors) {
        return motors != null && motors.stream()
                .filter(motor -> motor != null)
                .anyMatch(motor -> motorSubmissionType(motor) == Status.SUBMIT);
    }

    private Status motorStatus(PostCureMotor motor) {
        return motor.getMotorSubmissionStatus();
    }

    private void validateMotorPayload(PostCureMotor motor) {
        PostCureInhibitorType inhibitorType = motor.getInhibitorType();
        PostCureSectionPayloads.InhibitionDetails details = motor.getInhibitionDetails();

        if (inhibitorType == PostCureInhibitorType.NOT_APPLICABLE) {
            if (details == null) {
                return;
            }
            rejectIfNaMixSectionsPresent(motor.getMotorId(), details);
            return;
        }

        if (details == null) {
            return;
        }

        if (details.getNotApplicableRemarks() != null
                && !details.getNotApplicableRemarks().isBlank()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "notApplicableRemarks is allowed only for NOT_APPLICABLE motor "
                            + motor.getMotorId());
        }
    }

    private void rejectIfNaMixSectionsPresent(
            String motorId,
            PostCureSectionPayloads.InhibitionDetails details) {
        if (details.getPremixDetails() != null
                || details.getFinalMixDetails() != null
                || details.getQualificationDetails() != null
                || details.getInhibitorBatchDetails() != null
                || details.getDispatchDetails() != null
                || (details.getApplicationDetails() != null
                && !details.getApplicationDetails().isEmpty())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "NOT_APPLICABLE motor " + motorId
                            + " may only include notApplicableRemarks on inhibitionDetails");
        }
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
        batchService.assertStageUnlockedAndActive(batch, subDepartmentId, "Post Cure Operations");
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
                postCureRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                        .map(PostCureOperations::getMotors)
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
            PostCureOperations form,
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

    private Set<String> extractPostCureFileIds(List<PostCureMotor> motors) {
        Set<String> fileIds = new HashSet<>();
        if (motors == null || motors.isEmpty()) {
            return fileIds;
        }

        for (PostCureMotor motor : motors) {
            if (motor == null) {
                continue;
            }

            // 1. Loose Flap Filling Details -> Qualification Details -> qcReport
            if (motor.getLooseFlapFillingDetails() != null
                    && motor.getLooseFlapFillingDetails().getQualificationDetails() != null) {
                extractFileIdsFromDocs(
                        motor.getLooseFlapFillingDetails().getQualificationDetails().getQcReport(),
                        fileIds
                );
            }

            // 2. Inhibition Details -> Qualification Details -> qcReport
            if (motor.getInhibitionDetails() != null
                    && motor.getInhibitionDetails().getQualificationDetails() != null) {
                extractFileIdsFromDocs(
                        motor.getInhibitionDetails().getQualificationDetails().getQcReport(),
                        fileIds
                );
            }
        }
        return fileIds;
    }

    private void extractFileIdsFromDocs(List<DocumentDto> docs, Set<String> targetSet) {
        if (docs != null && !docs.isEmpty()) {
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

    private List<DocumentDto> promotePostCureFiles(
            List<PostCureMotor> motors,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        if (motors == null || motors.isEmpty()) {
            return newlyPromotedDocs;
        }

        for (PostCureMotor motor : motors) {
            if (motor == null) {
                continue;
            }

            // 1. Loose Flap Filling Details -> Qualification Details -> qcReport
            if (motor.getLooseFlapFillingDetails() != null
                    && motor.getLooseFlapFillingDetails().getQualificationDetails() != null) {
                List<DocumentDto> docs = motor.getLooseFlapFillingDetails().getQualificationDetails().getQcReport();
                if (docs != null && !docs.isEmpty()) {
                    newlyPromotedDocs.addAll(promoteDocumentList(docs, subDeptId, formId, existingFileIds, motor.getMotorId()));
                }
            }

            // 2. Inhibition Details -> Qualification Details -> qcReport
            if (motor.getInhibitionDetails() != null
                    && motor.getInhibitionDetails().getQualificationDetails() != null) {
                List<DocumentDto> docs = motor.getInhibitionDetails().getQualificationDetails().getQcReport();
                if (docs != null && !docs.isEmpty()) {
                    newlyPromotedDocs.addAll(promoteDocumentList(docs, subDeptId, formId, existingFileIds, motor.getMotorId()));
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

            if (existingFileIds.contains(fileId)) {
                fileMetadataEnrichmentService.enrichDocument(doc, subDeptId);
                return null;
            }

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
                return doc;

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

    private Set<String> extractMotorFileIds(PostCureMotor motor) {
        Set<String> fileIds = new HashSet<>();
        if (motor == null) {
            return fileIds;
        }
        if (motor.getLooseFlapFillingDetails() != null
                && motor.getLooseFlapFillingDetails().getQualificationDetails() != null) {
            fileIds.addAll(FileIdExtractionUtil.extractFromDocuments(
                    motor.getLooseFlapFillingDetails().getQualificationDetails().getQcReport()));
        }
        if (motor.getInhibitionDetails() != null
                && motor.getInhibitionDetails().getQualificationDetails() != null) {
            fileIds.addAll(FileIdExtractionUtil.extractFromDocuments(
                    motor.getInhibitionDetails().getQualificationDetails().getQcReport()));
        }
        return fileIds;
    }

    private void enrichPostCureMotorFiles(List<PostCureMotor> motors, Long subDeptId) {
        if (motors == null) {
            return;
        }
        for (PostCureMotor motor : motors) {
            if (motor == null) {
                continue;
            }
            if (motor.getLooseFlapFillingDetails() != null
                    && motor.getLooseFlapFillingDetails().getQualificationDetails() != null) {
                fileMetadataEnrichmentService.enrichDocuments(
                        motor.getLooseFlapFillingDetails().getQualificationDetails().getQcReport(),
                        subDeptId);
            }
            if (motor.getInhibitionDetails() != null
                    && motor.getInhibitionDetails().getQualificationDetails() != null) {
                fileMetadataEnrichmentService.enrichDocuments(
                        motor.getInhibitionDetails().getQualificationDetails().getQcReport(),
                        subDeptId);
            }
        }
    }

    private void rollbackPromotedFiles(List<DocumentDto> promotedDocs) {
        if (promotedDocs == null || promotedDocs.isEmpty()) {
            return;
        }
        for (DocumentDto doc : promotedDocs) {
            if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
                try {
                    fileBindingService.rollbackOrUnbindFile(doc.getFileId());
                } catch (Exception ex) {
                    log.warn("Failed to rollback file promotion for fileId: {}", doc.getFileId(), ex);
                }
            }
        }
    }

    // Helper to extract file IDs from existing DB entity during updates
    private Set<String> extractExistingFormFileIds(PostCureOperations form) {
        Set<String> fileIds = new HashSet<>();
        if (form == null || form.getMotors() == null) {
            return fileIds;
        }

        for (PostCureMotor motor : form.getMotors()) {
            if (motor == null) {
                continue;
            }

            if (motor.getLooseFlapFillingDetails() != null
                    && motor.getLooseFlapFillingDetails().getQualificationDetails() != null) {
                extractFileIdsFromDocs(
                        motor.getLooseFlapFillingDetails().getQualificationDetails().getQcReport(),
                        fileIds
                );
            }

            if (motor.getInhibitionDetails() != null
                    && motor.getInhibitionDetails().getQualificationDetails() != null) {
                extractFileIdsFromDocs(
                        motor.getInhibitionDetails().getQualificationDetails().getQcReport(),
                        fileIds
                );
            }
        }
        return fileIds;
    }

    /**
     * Ingredient recipe for a Post Cure mix table (loose-flap epoxy, IR-1, Hemcoat).
     */
    public PostCureIngredientsResponse getIngredients(String recipeType) {
        PostCureIngredientsResponse recipe = PostCureIngredientCatalog.resolve(recipeType);
        List<MaterialMaster> activeMaterials = materialRepository.findAll().stream()
                .filter(MaterialMasterDataService::isActive)
                .toList();

        return PostCureIngredientsResponse.builder()
                .recipeType(recipe.getRecipeType())
                .fixedPartsByWeight(recipe.isFixedPartsByWeight())
                .ingredients(resolveMaterials(recipe.getIngredients(), activeMaterials))
                .build();
    }

    private static List<PostCureIngredientDto> resolveMaterials(
            List<PostCureIngredientDto> ingredients,
            List<MaterialMaster> materials) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        List<PostCureIngredientDto> resolved = new ArrayList<>(ingredients.size());
        for (PostCureIngredientDto item : ingredients) {
            MaterialMaster match = findMaterialForIngredient(item, materials);
            resolved.add(PostCureIngredientDto.builder()
                    .ingredient(item.getIngredient())
                    .aliases(item.getAliases() != null ? item.getAliases() : List.of())
                    .partsByWeight(item.getPartsByWeight())
                    .materialId(match != null ? match.getMaterialId() : null)
                    .materialCode(match != null ? blankToNull(match.getMaterialCode()) : null)
                    .build());
        }
        return resolved;
    }

    private static MaterialMaster findMaterialForIngredient(
            PostCureIngredientDto item,
            List<MaterialMaster> materials) {
        Set<String> candidates = new LinkedHashSet<>();
        addNormalizedCandidate(candidates, item.getIngredient());
        if (item.getAliases() != null) {
            for (String alias : item.getAliases()) {
                addNormalizedCandidate(candidates, alias);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        for (MaterialMaster material : materials) {
            String nameKey = normalizeMaterialKey(material.getMaterialName());
            String codeKey = normalizeMaterialKey(material.getMaterialCode());
            if ((nameKey != null && candidates.contains(nameKey))
                    || (codeKey != null && candidates.contains(codeKey))) {
                return material;
            }
        }
        return null;
    }

    private static void addNormalizedCandidate(Set<String> candidates, String raw) {
        String key = normalizeMaterialKey(raw);
        if (key != null) {
            candidates.add(key);
        }
    }

    private static String normalizeMaterialKey(String value) {
        if (value == null) {
            return null;
        }
        String key = value.trim().toLowerCase(Locale.ROOT)
                .replace('₂', '2')
                .replace('₅', '5')
                .replaceAll("[^a-z0-9]+", "");
        return key.isEmpty() ? null : key;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
