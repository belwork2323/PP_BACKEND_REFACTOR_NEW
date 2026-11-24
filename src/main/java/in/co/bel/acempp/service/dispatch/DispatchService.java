package in.co.bel.acempp.service.dispatch;

import in.co.bel.acempp.dto.dispatch.*;
import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalRequest;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalResponse;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.dispatch.Dispatch;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.entity.qualitycontrol.DivisionDetail;
import in.co.bel.acempp.entity.qualitycontrol.QcDivision;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.dispatch.DispatchRepository;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionRepository;
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
public class DispatchService {

    private final DispatchRepository dispatchRepo;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final QcDivisionRepository qcDivisionRepo;
    private final BatchService batchService;
    private final MainBatchGateValidator mainBatchGateValidator;
    private final FormIdGeneratorService formIdGenerator;
    private final FileBindingService fileBindingService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;
    private final DispatchQcPopulateMapper dispatchQcPopulateMapper;

    /**
     * Seeds Dispatch fields from approved QC Division CASTING / NDT / QC (propellant) data.
     * Does not create or update a Dispatch form — UI applies the seed on first open.
     */
    public DispatchMotorDetailsResponse autoPopulateFromQcDivision(DispatchMotorDetailsRequest request) {
        String batchId = request.getBatchId() != null ? request.getBatchId().trim() : "";
        if (batchId.isBlank()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "batchId is required");
        }
        Batch batch = getBatch(batchId);

        QcDivision qcForm = qcDivisionRepo.findByBatchIdAndIsDeletedFalse(batchId).orElse(null);
        List<DivisionDetail> divisionDetails =
                qcForm != null && qcForm.getDivisionDetails() != null
                        ? qcForm.getDivisionDetails()
                        : Collections.emptyList();

        List<String> motorIds = resolvePopulateMotorIds(batch, request.getMotorId());
        List<DispatchMotorDetailsResponse.MotorSeed> seeds = new ArrayList<>();
        for (String motorId : motorIds) {
            dispatchQcPopulateMapper.buildSeed(divisionDetails, motorId).ifPresent(details ->
                    seeds.add(DispatchMotorDetailsResponse.MotorSeed.builder()
                            .motorId(motorId)
                            .dispatchDetails(details)
                            .build()));
        }

        return DispatchMotorDetailsResponse.builder()
                .batchId(batchId)
                .motors(seeds)
                .build();
    }

    private List<String> resolvePopulateMotorIds(Batch batch, String requestedMotorId) {
        String filter = requestedMotorId != null ? requestedMotorId.trim() : "";
        if (!filter.isBlank()) {
            return List.of(filter);
        }
        List<String> fromBatch = batch.getMotorIds();
        if (fromBatch != null && !fromBatch.isEmpty()) {
            return fromBatch.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(id -> !id.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public DispatchCreateResponse createForm(CreateDispatchRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Batch batch = getBatch(request.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());

        if (dispatchRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Dispatch form already submitted for this batch");
        }

        List<DispatchMotor> motors = prepareIncomingMotors(request.getMotors(), batch);
        Status batchStatus = calculateBatchStatus(motors, totalRequiredMotors(batch));
        User creator = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        String formId = formIdGenerator.generate("dispatches", "formId", "DISPATCH", 4);

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            // Promote uploaded photos & clearance certificates
            promotedDocs = promoteDispatchFiles(
                    request.getMotors(),
                    request.getSubDepartmentId(),
                    formId,
                    Collections.emptySet()
            );

            Dispatch entity = Dispatch.builder()
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

            Dispatch savedForm = dispatchRepo.save(entity);
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
            DispatchCreateResponse.DispatchCreateResponseBuilder response =
                    DispatchCreateResponse.builder()
                            .formId(savedForm.getFormId())
                            .batchId(savedForm.getBatchId())
                            .formStatus(batchStatus)
                            .motorCount(motors.size())
                            .motors(motors.stream()
                                    .map(motor -> DispatchMotorSummary.builder()
                                            .motorId(motor.getMotorId())
                                            .build())
                                    .collect(Collectors.toList()))
                            .pendingMotorCount(counts.pending())
                            .approvedMotorCount(counts.approved())
                            .rejectedMotorCount(counts.rejected())
                            .inProgressMotorCount(counts.inProgress())
                            .totalMotorCount(totalRequiredMotors(batch))
                            .motorStatuses(buildFullMotorStatuses(
                                    batch, motors, batch.getMotorIds()));

            if (isDraft) {
                response.createdBy(userId)
                        .createdOn(now);
            } else {
                response.submittedBy(userId)
                        .submittedOn(now);
            }

            log.info("Dispatch form {} created by {}", formId, userId);
            return response.build();

        } catch (Exception ex) {
            log.error("Dispatch creation failed for formId={}. Initiating file rollback...", formId, ex);
            rollbackPromotedFiles(promotedDocs);
            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to create Dispatch form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public DispatchUpdateResponse updateForm(UpdateDispatchRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Dispatch form = dispatchRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Dispatch form not found for formId: " + request.getFormId()));

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

        Map<String, DispatchMotor> existingMotorMap = form.getMotors() == null
                ? new LinkedHashMap<>()
                : form.getMotors().stream().collect(Collectors.toMap(
                DispatchMotor::getMotorId,
                Function.identity(),
                (first, ignored) -> first,
                LinkedHashMap::new));

        List<DispatchMotor> incomingMotors = prepareIncomingMotors(request.getMotors(), batch);

        if (!isFinalFormSubmit) {
            for (DispatchMotor incoming : incomingMotors) {
                DispatchMotor existing = existingMotorMap.get(incoming.getMotorId());
                if (existing != null && isProtectedMotor(existing)) {
                    throw new AcemException(
                            ErrorCode.FORM_ALREADY_EXISTS,
                            "Motor " + incoming.getMotorId() + " cannot be modified.");
                }
            }
        }

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            // 1. Extract existing DB file IDs vs incoming payload file IDs
            Set<String> existingDbFileIds = extractDispatchFileIdsFromEntities(form.getMotors());
            Set<String> incomingFileIds = extractDispatchFileIdsFromRequests(request.getMotors());

            // 2. Identify removed files
            List<String> removedFileIds = existingDbFileIds.stream()
                    .filter(fileId -> !incomingFileIds.contains(fileId))
                    .collect(Collectors.toList());

            // 3. Promote NEW files
            promotedDocs = promoteDispatchFiles(
                    request.getMotors(),
                    request.getSubDepartmentId(),
                    form.getFormId(),
                    existingDbFileIds
            );

            // 4. Purge removed files from disk and DB
            if (!removedFileIds.isEmpty()) {
                fileBindingService.purgeRemovedFilesFromDiskAndDb(form.getFormId(), removedFileIds);
            }

            for (DispatchMotor incoming : incomingMotors) {
                DispatchMotor existing = existingMotorMap.get(incoming.getMotorId());
                if (existing == null || !isProtectedMotor(existing)) {
                    incoming.setRejectionReason(null);
                    existingMotorMap.put(incoming.getMotorId(), incoming);
                }
            }

            List<DispatchMotor> updatedMotors = new ArrayList<>(existingMotorMap.values());
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

            Dispatch savedForm = dispatchRepo.save(form);
            updateBatchStage(
                    batch,
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    isFinalFormSubmit || hasSubmittedMotor(incomingMotors),
                    userId,
                    updater.getUsername(),
                    now);

            MotorCounts counts = countMotors(updatedMotors);
            DispatchUpdateResponse.DispatchUpdateResponseBuilder response =
                    DispatchUpdateResponse.builder()
                            .formId(savedForm.getFormId())
                            .batchId(savedForm.getBatchId())
                            .formStatus(batchStatus)
                            .motorCount(updatedMotors.size())
                            .motors(updatedMotors.stream()
                                    .map(motor -> DispatchMotorSummary.builder()
                                            .motorId(motor.getMotorId())
                                            .build())
                                    .collect(Collectors.toList()))
                            .updatedBy(userId)
                            .updatedOn(now)
                            .pendingMotorCount(counts.pending())
                            .approvedMotorCount(counts.approved())
                            .rejectedMotorCount(counts.rejected())
                            .inProgressMotorCount(counts.inProgress())
                            .totalMotorCount(totalRequiredMotors(batch))
                            .motorStatuses(buildFullMotorStatuses(
                                    batch, updatedMotors, batch.getMotorIds()));

            if (isFinalFormSubmit) {
                response.submittedBy(userId)
                        .submittedOn(now);
            }

            log.info("Dispatch form {} updated by {}", savedForm.getFormId(), userId);
            return response.build();

        } catch (Exception ex) {
            log.error("Dispatch update failed for formId={}. Initiating file rollback...", form.getFormId(), ex);
            rollbackPromotedFiles(promotedDocs);
            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update Dispatch form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public DispatchDetailsResponse getFormDetails(String formId) {
        Dispatch form = dispatchRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Dispatch form not found for formId: " + formId));
        Batch batch = getBatch(form.getBatchId());
        List<DispatchMotor> motors = form.getMotors() != null
                ? form.getMotors()
                : Collections.emptyList();
        enrichDispatchMotorFiles(motors, form.getSubDepartmentId());
        MotorCounts counts = countMotors(motors);
        int totalMotorCount = totalRequiredMotors(batch);

        return DispatchDetailsResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .batchType(form.getBatchType())
                .subDepartmentId(form.getSubDepartmentId())
                .formSubmissionType(form.getFormSubmissionType())
                .formStatus(form.getStatus())
                .createdBy(batch.getCreatedBy() != null
                        ? batch.getCreatedBy().getId()
                        : null)
                .createdAt(batch.getCreatedAt())
                .submittedBy(form.getCreatedBy())
                .submittedAt(form.getCreatedAt())
                .updatedBy(form.getUpdatedBy())
                .updatedAt(form.getUpdatedAt())
                .motors(motors)
                .allMotorsApproved(counts.approved() == totalMotorCount)
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .inProgressMotorCount(counts.inProgress())
                .totalMotorCount(totalMotorCount)
                .motorStatuses(buildFullMotorStatuses(
                        batch, motors, batch.getMotorIds()))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public MotorApprovalResponse changeStatusOfMotor(
            MotorApprovalRequest request,
            String approverUserId) {

        validateMotorApprovalRequest(request);
        Dispatch form = dispatchRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Dispatch form not found"));

        if (request.getSubDepartmentId() != null
                && !request.getSubDepartmentId().equals(form.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The motor approval request does not belong to Dispatch");
        }
        if (form.getMotors() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "No motors are available in this form");
        }

        DispatchMotor motor = form.getMotors().stream()
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
        Status batchStatus =
                calculateBatchStatus(form.getMotors(), totalMotorCount);
        form.setStatus(batchStatus);
        if (batchStatus == Status.COMPLETELY_APPROVED) {
            form.setFormSubmissionType(Status.SUBMIT);
            form.setRejectionReason(null);
        }

        if (request.getActionType() == Status.APPROVED) {
            fileReadOnlyService.markReadOnly(form.getSubDepartmentId(), extractMotorFileIds(motor));
            try {
                Map<String, Object> blockchainPayload = buildMotorBlockchainPayload(form, motor, approverUserId , now);
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

        dispatchRepo.save(form);
        updateBatchAfterMotorApproval(
                batch, form, batchStatus, approver, now);

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

    private Map<String, Object> buildMotorBlockchainPayload(Dispatch form, DispatchMotor motor, String approverName, LocalDateTime approvalTime) {
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
            motorMap.put("motorSubmissionType", motor.getMotorSubmissionType());
            motorMap.put("motorSubmissionStatus", motor.getMotorSubmissionStatus());

            if (motor.getDispatchDetails() != null) {
                var dispatch = motor.getDispatchDetails();
                Map<String, Object> dispatchMap = new LinkedHashMap<>();
                dispatchMap.put("stage", dispatch.getStage());
                dispatchMap.put("castingDate", dispatch.getCastingDate());
                dispatchMap.put("dispatchDate", dispatch.getDispatchDate());
                dispatchMap.put("dispatchLocation", dispatch.getDispatchLocation());
                dispatchMap.put("ndtClearance", dispatch.getNdtClearance());
                dispatchMap.put("finalAcceptanceCommitteeClearance", dispatch.getFinalAcceptanceCommitteeClearance());
                dispatchMap.put("propellantProperties", dispatch.getPropellantProperties() == null ? Collections.emptyList() : dispatch.getPropellantProperties());
                dispatchMap.put("waiverDetails", dispatch.getWaiverDetails());
                dispatchMap.put("rocketMotorInspection", dispatch.getRocketMotorInspection() == null ? Collections.emptyList() : dispatch.getRocketMotorInspection());
                dispatchMap.put("vehicleDetails", dispatch.getVehicleDetails() == null ? Collections.emptyList() : dispatch.getVehicleDetails());
                dispatchMap.put("rocketMotorPackingDetails", dispatch.getRocketMotorPackingDetails() == null ? Collections.emptyList() : dispatch.getRocketMotorPackingDetails());
                dispatchMap.put("safetyClearance", dispatch.getSafetyClearance());
                dispatchMap.put("dispatchTeam", dispatch.getDispatchTeam());

                motorMap.put("dispatchDetails", dispatchMap);
            }
        }
        rootPayload.put("motorDetails", motorMap);

        return rootPayload;
    }
    public ApproverStatusResponse changeStatus(
            ApproverStatusRequest request,
            String approverUserId) {

        Dispatch entity = dispatchRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "Not found",
                        "Dispatch form not found: " + request.getFormId()));
        User approver = getUser(approverUserId);

        ApprovalStatusUtil.requireLegacyFinalApprovalState(entity.getStatus());
        ApprovalStatusUtil.applyStatusChange(request.getActionType(), request.getSubDepartmentId(), request.getRemarks(), request.getRejectionReason(),
                approver.getUsername(), entity::setStatus,
                entity::setRemarks, entity::setRejectionReason, entity::setUpdatedBy, entity::setUpdatedAt
        );
        entity.setUpdatedBy(PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build());

        if (request.getActionType() == Status.APPROVED
                || request.getActionType() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(entity.getFormId(), entity.getSubDepartmentId());
        }

        dispatchRepo.save(entity);
//        if (request.getActionType() == Status.APPROVED) {
//            approvalBlockchainAssetService.createOnApprovedStatus(
//                    entity.getSubDepartmentId(),
//                    entity.getBatchId(),
//                    entity.getFormId(),
//                    entity,
//                    approverUserId);
//        }

        batchService.updateBatchStatus(entity.getBatchId(), entity.getSubDepartmentId(), entity.getStatus(),
                entity.getRemarks(), entity.getRejectionReason(), approverUserId);
        return ApproverStatusResponse.builder()
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionAt(entity.getUpdatedAt())
                .actionBy(entity.getUpdatedBy())
                .build();
    }

    private List<DispatchMotor> prepareIncomingMotors(
            List<DispatchMotorRequest> motorForms,
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

        Map<String, DispatchMotor> motorsById = new LinkedHashMap<>();
        for (DispatchMotorRequest motorForm : motorForms) {
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

            Status submissionType = motorForm.getMotorSubmissionType();
            if (submissionType != Status.DRAFT
                    && submissionType != Status.SUBMIT) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorSubmissionType must be DRAFT or SUBMIT for motor "
                                + motorId);
            }
            mainBatchGateValidator.assertMotorUnlocked(
                    batch, MainBatchStageConstants.DISPATCH, motorId);
            if (submissionType == Status.SUBMIT) {
                mainBatchGateValidator.assertDispatchMotorCanBeSubmitted(batch, motorId);
            }

            motorsById.put(
                    motorId,
                    DispatchMotor.builder()
                            .motorId(motorId)
                            .motorSubmissionType(submissionType)
                            .motorSubmissionStatus(submissionType == Status.DRAFT
                                    ? Status.IN_PROGRESS
                                    : Status.WAITING_FOR_APPROVAL)
                            .rejectionReason(null)
                            .dispatchDetails(motorForm.getDispatchDetails())
                            .build());
        }
        return new ArrayList<>(motorsById.values());
    }

    private static String resolveFormRejectionReasonFromMotors(List<DispatchMotor> motors) {
        if (motors == null) {
            return null;
        }
        return motors.stream()
                .filter(m -> m != null && m.getMotorSubmissionStatus() == Status.REJECTED)
                .map(DispatchMotor::getRejectionReason)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<MotorStatusResponse> buildFullMotorStatuses(
            Batch batch,
            List<DispatchMotor> savedMotors,
            List<String> batchMotorIds) {

        Map<String, DispatchMotor> savedMap = savedMotors == null
                ? Collections.emptyMap()
                : savedMotors.stream().collect(Collectors.toMap(
                DispatchMotor::getMotorId,
                Function.identity(),
                (first, ignored) -> first));

        List<MotorStatusResponse> statuses = new ArrayList<>();
        for (String motorId : batchMotorIds) {
            DispatchMotor motor = savedMap.get(motorId);
            statuses.add(motor == null
                    ? MotorStatusResponse.builder()
                    .motorId(motorId)
                    .motorSubmissionType(Status.DRAFT)
                    .motorSubmissionStatus(MainBatchStageProgressHelper.defaultMotorSubmissionStatus(
                            batch, MainBatchStageConstants.DISPATCH, motorId))
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
            List<DispatchMotor> savedMotors,
            int totalRequiredMotors) {

        if (savedMotors == null || savedMotors.isEmpty()) {
            return Status.TO_BE_INITIATED;
        }
        if (savedMotors.stream()
                .anyMatch(motor -> motorStatus(motor) == Status.REJECTED)) {
            return Status.REJECTED;
        }

        long waitingCount = savedMotors.stream()
                .filter(motor ->
                        motorStatus(motor) == Status.WAITING_FOR_APPROVAL)
                .count();
        long approvedCount = savedMotors.stream()
                .filter(motor -> motorStatus(motor) == Status.APPROVED)
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
                .anyMatch(motor -> motorStatus(motor) == Status.IN_PROGRESS
                        || motorSubmissionType(motor) == Status.DRAFT);
        return anyStarted ? Status.IN_PROGRESS : Status.TO_BE_INITIATED;
    }

    private MotorCounts countMotors(List<DispatchMotor> motors) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int inProgress = 0;
        for (DispatchMotor motor : motors) {
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
        return new MotorCounts(
                pending, approved, rejected, inProgress);
    }

    private boolean isProtectedMotor(DispatchMotor motor) {
        Status status = motorStatus(motor);
        return status == Status.APPROVED
                || status == Status.WAITING_FOR_APPROVAL;
    }

    private Status motorSubmissionType(DispatchMotor motor) {
        return motor.getMotorSubmissionType();
    }

    private boolean hasSubmittedMotor(List<DispatchMotor> motors) {
        return motors != null && motors.stream()
                .filter(Objects::nonNull)
                .anyMatch(motor -> motorSubmissionType(motor) == Status.SUBMIT);
    }

    private Status motorStatus(DispatchMotor motor) {
        return motor.getMotorSubmissionStatus();
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

    private void validateCurrentStage(
            Batch batch,
            Long subDepartmentId) {
        batchService.assertStageUnlockedAndActive(batch, subDepartmentId, "Dispatch");
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
                dispatchRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                        .map(Dispatch::getMotors)
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
            Dispatch form,
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


    private List<DocumentDto> promoteDispatchFiles(
            List<DispatchMotorRequest> motors,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        if (motors == null || motors.isEmpty()) {
            return newlyPromotedDocs;
        }

        for (DispatchMotorRequest motor : motors) {
            if (motor == null || motor.getDispatchDetails() == null) {
                continue;
            }

            DispatchPayloads.DispatchDetails details = motor.getDispatchDetails();

            // 1. Promote Safety Clearance Certificate
            if (details.getSafetyClearance() != null && details.getSafetyClearance().getClearanceCertificate() != null) {
                promoteSingleFile(
                        details.getSafetyClearance().getClearanceCertificate(),
                        subDeptId, formId, existingFileIds, motor.getMotorId(), newlyPromotedDocs);
            }

            // 2. Promote Uploaded Dispatch Photos List
            if (details.getUploadDispatchPhotos() != null && !details.getUploadDispatchPhotos().isEmpty()) {
                for (DocumentDto photo : details.getUploadDispatchPhotos()) {
                    promoteSingleFile(photo, subDeptId, formId, existingFileIds, motor.getMotorId(), newlyPromotedDocs);
                }
            }
        }

        return newlyPromotedDocs;
    }

    /**
     * Promotes a single file attachment via FileBindingService and updates the DocumentDto metadata.
     * If the file is already bound to this form, its metadata is restored from the DB.
     */
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

        if (existingFileIds != null && existingFileIds.contains(fileId)) {
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
            log.error("File promotion failed for fileId: {} bound to formId: {}", fileId, formId, e);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload promotion failed for fileId " + fileId + ": " + e.getMessage()
            );
        }
    }

    private Set<String> extractMotorFileIds(DispatchMotor motor) {
        return extractDispatchFileIdsFromEntities(motor == null ? null : List.of(motor));
    }

    private void enrichDispatchMotorFiles(List<DispatchMotor> motors, Long subDeptId) {
        if (motors == null) {
            return;
        }
        for (DispatchMotor motor : motors) {
            if (motor == null || motor.getDispatchDetails() == null) {
                continue;
            }
            DispatchPayloads.DispatchDetails details = motor.getDispatchDetails();
            if (details.getSafetyClearance() != null && details.getSafetyClearance().getClearanceCertificate() != null) {
                fileMetadataEnrichmentService.enrichDocument(
                        details.getSafetyClearance().getClearanceCertificate(), subDeptId);
            }
            if (details.getUploadDispatchPhotos() != null) {
                fileMetadataEnrichmentService.enrichDocuments(details.getUploadDispatchPhotos(), subDeptId);
            }
        }
    }

    private void rollbackPromotedFiles(List<DocumentDto> promotedDocs) {
        if (promotedDocs == null || promotedDocs.isEmpty()) {
            return;
        }

        log.info("Initiating rollback for {} promoted file(s)...", promotedDocs.size());
        for (DocumentDto doc : promotedDocs) {
            if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
                try {
                    fileBindingService.rollbackOrUnbindFile(doc.getFileId());
                    log.info("Successfully rolled back fileId: {}", doc.getFileId());
                } catch (Exception e) {
                    log.error("Failed to execute file rollback for fileId: {}", doc.getFileId(), e);
                }
            }
        }
    }


    private Set<String> extractDispatchFileIdsFromEntities(List<DispatchMotor> motors) {
        Set<String> fileIds = new HashSet<>();
        if (motors == null || motors.isEmpty()) {
            return fileIds;
        }

        for (DispatchMotor motor : motors) {
            if (motor == null || motor.getDispatchDetails() == null) {
                continue;
            }

            DispatchPayloads.DispatchDetails details = motor.getDispatchDetails();

            if (details.getSafetyClearance() != null && details.getSafetyClearance().getClearanceCertificate() != null) {
                DocumentDto doc = details.getSafetyClearance().getClearanceCertificate();
                if (doc.getFileId() != null && !doc.getFileId().isBlank()) {
                    fileIds.add(doc.getFileId());
                }
            }

            if (details.getUploadDispatchPhotos() != null) {
                for (DocumentDto photo : details.getUploadDispatchPhotos()) {
                    if (photo != null && photo.getFileId() != null && !photo.getFileId().isBlank()) {
                        fileIds.add(photo.getFileId());
                    }
                }
            }
        }
        return fileIds;
    }

    private Set<String> extractDispatchFileIdsFromRequests(List<DispatchMotorRequest> motors) {
        Set<String> fileIds = new HashSet<>();
        if (motors == null || motors.isEmpty()) {
            return fileIds;
        }

        for (DispatchMotorRequest motor : motors) {
            if (motor == null || motor.getDispatchDetails() == null) {
                continue;
            }

            DispatchPayloads.DispatchDetails details = motor.getDispatchDetails();

            if (details.getSafetyClearance() != null && details.getSafetyClearance().getClearanceCertificate() != null) {
                DocumentDto doc = details.getSafetyClearance().getClearanceCertificate();
                if (doc.getFileId() != null && !doc.getFileId().isBlank()) {
                    fileIds.add(doc.getFileId());
                }
            }

            if (details.getUploadDispatchPhotos() != null) {
                for (DocumentDto photo : details.getUploadDispatchPhotos()) {
                    if (photo != null && photo.getFileId() != null && !photo.getFileId().isBlank()) {
                        fileIds.add(photo.getFileId());
                    }
                }
            }
        }
        return fileIds;
    }

}
