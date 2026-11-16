package in.co.bel.acempp.service.qualitycontrol;

import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalRequest;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalResponse;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.subscale.subscalecreation.TrimmingTableDTO;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.statictestingfacility.*;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.manufacturing.SubscaleProcessing;
import in.co.bel.acempp.entity.qualitycontrol.StaticTesting;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.FileSystemRepository;
import in.co.bel.acempp.repository.manufacturing.SubscaleProcessingRepository;
import in.co.bel.acempp.repository.qualitycontrol.BemMotorRepository;
import in.co.bel.acempp.repository.qualitycontrol.StaticTestingRepository;
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
import in.co.bel.acempp.service.SubscaleMetadataResolver;
import in.co.bel.acempp.dto.subscale.subscalecreation.HardwarePreparationDetailsDTO;
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
public class StaticTestingService {

    private static final StfMotorSubType MAIN_MOTOR = StfMotorSubType.MAIN_MOTOR;
    private static final StfMotorSubType BEM = StfMotorSubType.BEM;

    private final StaticTestingRepository stfRepo;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final SubscaleProcessingRepository subscaleRepo;
    private final BatchService batchService;
    private final FormIdGeneratorService formIdGenerator;
    private final BemMotorRepository bemMotorRepository;
    private final FileBindingService fileBindingService;
    private final FileSystemRepository fileSystemRepository;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final MainBatchGateValidator mainBatchGateValidator;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;

    public StfCreateResponse createForm(CreateStfRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Batch batch = getBatch(request.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());
        validateFormSubmissionType(request.getFormSubmissionType());
        if (request.getFormSubmissionType() == Status.SUBMIT) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "A new STF form cannot be finally submitted before all motors are approved");
        }
        if (stfRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Static testing form already exists for this batch");
        }

        LinkedHashMap<String, StfMotorSubType> eligibleMotors = eligibleMotors(batch);
        List<StaticMotorData> motors = prepareIncomingMotors(
                batch, request.getMotors(), eligibleMotors, false);
        Status formStatus = calculateFormStatus(motors, eligibleMotors.size());
        User creator = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        String formId = formIdGenerator.generate("static_testings", "formId", "STF", 3);

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            // Promote uploaded files and track them
            promotedDocs = promoteStfFiles(motors, request.getSubDepartmentId(), formId, Collections.emptySet());

            StaticTesting savedForm = stfRepo.save(StaticTesting.builder()
                    .formId(formId)
                    .batchId(batch.getBatchId())
                    .subDepartmentId(request.getSubDepartmentId())
                    .formSubmissionType(Status.DRAFT)
                    .status(formStatus)
                    .motors(motors)
                    .createdBy(person(creator))
                    .createdAt(now)
                    .build());

            List<MotorStatusResponse> motorStatuses = buildFullMotorStatuses(batch, motors, eligibleMotors);
            syncStage(batch, savedForm, formStatus, motorStatuses,
                    hasSubmittedMotor(motors) ? person(creator) : null,
                    now, false, null);

            MotorCounts counts = countMotors(motors);
            log.info("Static Testing form {} created by {}", formId, userId);
            return StfCreateResponse.builder()
                    .formId(formId)
                    .batchId(batch.getBatchId())
                    .formStatus(formStatus)
                    .motorCount(motors.size())
                    .motors(motorIds(motors))
                    .createdBy(userId)
                    .createdOn(now)
                    .allMotorsApproved(allEligibleMotorsApproved(motors, eligibleMotors))
                    .pendingMotorCount(counts.pending())
                    .approvedMotorCount(counts.approved())
                    .rejectedMotorCount(counts.rejected())
                    .inProgressMotorCount(counts.inProgress())
                    .totalMotorCount(eligibleMotors.size())
                    .motorStatuses(motorStatuses)
                    .build();

        } catch (Exception ex) {
            log.error("Form creation failed for formId={}. Initiating file promotion rollback...", formId, ex);
            rollbackPromotedFiles(promotedDocs);
            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to create STF form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    // ==========================================
// 2. FILE PROMOTION, PURGE & ROLLBACK IN UPDATE FORM
// ==========================================
    public StfUpdateResponse updateForm(UpdateStfRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        StaticTesting form = stfRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Static testing form not found for formId: " + request.getFormId()));
        if (form.getStatus() == Status.COMPLETELY_APPROVED) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Static testing form cannot be modified in status " + form.getStatus());
        }
        if (!Objects.equals(form.getBatchId(), request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The form does not belong to batch " + request.getBatchId());
        }
        if (!Objects.equals(form.getSubDepartmentId(), request.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The form does not belong to the requested subdepartment");
        }

        Batch batch = getBatch(form.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());
        validateFormSubmissionType(request.getFormSubmissionType());
        LinkedHashMap<String, StfMotorSubType> eligibleMotors = eligibleMotors(batch);
        User updater = getUser(userId);
        LocalDateTime now = LocalDateTime.now();

        if (request.getFormSubmissionType() == Status.SUBMIT) {
            List<StaticMotorData> savedMotors = safeMotors(form);
            MotorCounts counts = countMotors(savedMotors);
            if (!allEligibleMotorsApproved(savedMotors, eligibleMotors)) {
                throw new AcemException(
                        ErrorCode.INVALID_STATE,
                        "All eligible main and BEM motors must be approved before final STF submission");
            }

            form.setFormSubmissionType(Status.SUBMIT);
            form.setStatus(Status.COMPLETELY_APPROVED);
            form.setUpdatedBy(person(updater));
            form.setUpdatedAt(now);
            form.setRemarks(null);
            form.setRejectionReason(null);
            StaticTesting savedForm = stfRepo.save(form);
            List<MotorStatusResponse> motorStatuses = buildFullMotorStatuses(batch, savedMotors, eligibleMotors);
            syncStage(batch, savedForm, Status.COMPLETELY_APPROVED,
                    motorStatuses, person(updater), now, false, null);
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());

            return buildUpdateResponse(savedForm, savedMotors, eligibleMotors,
                    motorStatuses, counts, userId, now, true);
        }

        List<DocumentDto> promotedDocs = new ArrayList<>();

        try {
            List<StaticMotorData> incomingMotors = prepareIncomingMotors(
                    batch, request.getMotors(), eligibleMotors, true);
            LinkedHashMap<String, StaticMotorData> existingById = safeMotors(form).stream()
                    .collect(Collectors.toMap(
                            StaticMotorData::getMotorId,
                            Function.identity(),
                            (first, ignored) -> first,
                            LinkedHashMap::new));

            for (StaticMotorData incoming : incomingMotors) {
                StaticMotorData existing = existingById.get(incoming.getMotorId());
                if (existing != null && isProtectedMotor(existing)) {
                    throw new AcemException(
                            ErrorCode.FORM_ALREADY_EXISTS,
                            "Motor " + incoming.getMotorId() + " cannot be modified while it is waiting or approved");
                }
                existingById.put(incoming.getMotorId(), incoming);
            }

            List<StaticMotorData> updatedMotors = new ArrayList<>(existingById.values());
            Status formStatus = calculateFormStatus(updatedMotors, eligibleMotors.size());

            // Extract existing vs incoming file IDs
            Set<String> existingDbFileIds = extractStfFileIds(form.getMotors());
            Set<String> incomingFileIds = extractStfFileIds(updatedMotors);

            // Identify purged files
            List<String> removedFileIds = existingDbFileIds.stream()
                    .filter(fileId -> !incomingFileIds.contains(fileId))
                    .collect(Collectors.toList());

            // Promote NEW files only
            promotedDocs = promoteStfFiles(
                    updatedMotors,
                    request.getSubDepartmentId(),
                    request.getFormId(),
                    existingDbFileIds
            );

            // Delete removed files from disk/DB
            if (!removedFileIds.isEmpty()) {
                fileBindingService.purgeRemovedFilesFromDiskAndDb(request.getFormId(), removedFileIds);
            }

            form.setFormSubmissionType(Status.DRAFT);
            form.setStatus(formStatus);
            form.setMotors(updatedMotors);
            form.setUpdatedBy(person(updater));
            form.setUpdatedAt(now);
            form.setRemarks(null);
            form.setRejectionReason(null);
            StaticTesting savedForm = stfRepo.save(form);

            List<MotorStatusResponse> motorStatuses = buildFullMotorStatuses(batch, updatedMotors, eligibleMotors);
            syncStage(batch, savedForm, formStatus, motorStatuses,
                    hasSubmittedMotor(incomingMotors) ? person(updater) : null,
                    now, false, null);
            MotorCounts counts = countMotors(updatedMotors);
            log.info("Static Testing form {} updated by {}", savedForm.getFormId(), userId);
            return buildUpdateResponse(savedForm, updatedMotors, eligibleMotors,
                    motorStatuses, counts, userId, now, false);

        } catch (Exception ex) {
            log.error("Form update failed for formId={}. Initiating file promotion rollback...", request.getFormId(), ex);
            rollbackPromotedFiles(promotedDocs);
            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update STF form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    // ==========================================
// 3. HELPER METHODS FOR FILE MANAGEMENT
// ==========================================
    private List<DocumentDto> promoteStfFiles(
            List<StaticMotorData> motors,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        if (motors == null || motors.isEmpty()) {
            return newlyPromotedDocs;
        }

        for (StaticMotorData motor : motors) {
            if (motor == null || motor.getStaticTestingDetails() == null) {
                continue;
            }

            // MAIN/BEM STF sheets no longer carry ptCurveFile attachments.
        }
        return newlyPromotedDocs;
    }

    private void promoteSingleFile(
            DocumentDto report,
            Long deptId,
            String formId,
            Set<String> existingFileIds,
            List<DocumentDto> newlyPromotedList,
            StaticMotorData motor) {

        if (report == null || report.getFileId() == null || report.getFileId().isBlank()) {
            return;
        }

        String fileId = report.getFileId().trim();

        // SCENARIO 1: File is already bound -> Enrich metadata from DB without re-promoting
        if (existingFileIds.contains(fileId)) {
            fileMetadataEnrichmentService.enrichDocument(report, deptId);
            return; // Do NOT add to newlyPromotedList (protects existing files during transaction rollback)
        }

        // SCENARIO 2: New Temp File -> Promote & Bind to form
        try {
            String bemNo = BEM.equals(motor.getSubType()) ? motor.getMotorId() : null;
            FileAttachments savedFile = fileBindingService.promoteAndBindFile(
                    fileId,
                    deptId,
                    formId,
                    FileBindingScope.forStf(
                            motor.getMotorId(),
                            motor.getStfTestNo(),
                            motor.getSubType().name(),
                            bemNo)
            );
            report.setFileUrl(savedFile.getFilePath());
            report.setFileName(savedFile.getOriginalFileName());
            report.setStatus(savedFile.getStatus());
            report.setFileId(savedFile.getFileId());
            report.setMimeType(savedFile.getMimeType());
            report.setReadOnly(savedFile.isReadOnly());
            newlyPromotedList.add(report); // Track newly promoted file for rollback handling

        } catch (Exception e) {
            log.error("File promotion failed for fileId: {}", fileId, e);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload failed, please try again."
            );
        }
    }


    private Set<String> extractMotorFileIds(StaticMotorData motor) {
        return extractStfFileIds(List.of(motor));
    }

    private void enrichStfMotorFiles(List<StaticMotorData> motors, Long subDepartmentId) {
        if (motors == null) {
            return;
        }
        for (StaticMotorData motor : motors) {
            if (motor == null) {
                continue;
            }
            if (BEM.equals(motor.getSubType())) {
                motor.setBemNo(motor.getMotorId());
            }
            if (motor.getStaticTestingDetails() == null) {
                continue;
            }
            // No STF file attachments on the new MAIN/BEM shapes.
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

    private Set<String> extractStfFileIds(List<StaticMotorData> motors) {
        Set<String> fileIds = new HashSet<>();
        if (motors == null || motors.isEmpty()) {
            return fileIds;
        }

        for (StaticMotorData motor : motors) {
            if (motor == null || motor.getStaticTestingDetails() == null) {
                continue;
            }

            // No STF file attachments on the new MAIN/BEM shapes.
        }
        return fileIds;
    }

    public StfDetailsResponse getFormDetails(String formId) {
        StaticTesting form = stfRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Static testing form not found for formId: " + formId));
        Batch batch = getBatch(form.getBatchId());
        LinkedHashMap<String, StfMotorSubType> eligibleMotors = eligibleMotors(batch);
        List<StaticMotorData> motors = safeMotors(form);
        MotorCounts counts = countMotors(motors);
        enrichStfMotorFiles(motors, form.getSubDepartmentId());

        return StfDetailsResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .subDepartmentId(form.getSubDepartmentId())
                .formSubmissionType(form.getFormSubmissionType())
                .formStatus(form.getStatus())
                .createdBy(batch.getCreatedBy() != null ? batch.getCreatedBy().getId() : null)
                .createdAt(batch.getCreatedAt())
                .submittedBy(form.getCreatedBy())
                .submittedAt(form.getCreatedAt())
                .updatedBy(form.getUpdatedBy())
                .updatedAt(form.getUpdatedAt())
                .motors(motors)
                .allMotorsApproved(allEligibleMotorsApproved(motors, eligibleMotors))
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .inProgressMotorCount(counts.inProgress())
                .totalMotorCount(eligibleMotors.size())
                .motorStatuses(buildFullMotorStatuses(batch, motors, eligibleMotors))
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public MotorApprovalResponse changeStatusOfMotor(
            MotorApprovalRequest request,
            String approverUserId) {
        validateMotorApprovalRequest(request);
        StaticTesting form = stfRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Static testing form not found"));
        if (request.getSubDepartmentId() != null
                && !Objects.equals(request.getSubDepartmentId(), form.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The motor approval request does not belong to STF");
        }

        StaticMotorData motor = safeMotors(form).stream()
                .filter(item -> Objects.equals(item.getMotorId(), request.getMotorId()))
                .findFirst()
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor not found in this STF form"));
        if (motorStatus(motor) != Status.WAITING_FOR_APPROVAL) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Motor is not waiting for approval");
        }

        motor.setMotorSubmissionStatus(request.getActionType());
        User approver = getUser(approverUserId);
        LocalDateTime now = LocalDateTime.now();
        form.setUpdatedBy(person(approver));
        form.setUpdatedAt(now);
        form.setRemarks(request.getActionType() == Status.APPROVED ? request.getRemarks() : null);
        form.setRejectionReason(request.getActionType() == Status.REJECTED
                ? request.getRejectionReason() : null);

        Batch batch = getBatch(form.getBatchId());
        LinkedHashMap<String, StfMotorSubType> eligibleMotors = eligibleMotors(batch);
        Status formStatus = calculateFormStatus(safeMotors(form), eligibleMotors.size());
        form.setStatus(formStatus);
        if (formStatus == Status.COMPLETELY_APPROVED) {
            form.setFormSubmissionType(Status.SUBMIT);
            form.setRejectionReason(null);
        }
        stfRepo.save(form);

        List<MotorStatusResponse> motorStatuses = buildFullMotorStatuses(batch, form.getMotors(), eligibleMotors);
        syncStage(batch, form, formStatus, motorStatuses, null, now,
                request.getActionType() == Status.APPROVED, approverUserId);
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
        if (formStatus == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }
        MotorCounts counts = countMotors(form.getMotors());

        return MotorApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .motorId(motor.getMotorId())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .motorSubmissionStatus(motorStatus(motor))
                .batchStatus(formStatus)
                .allMotorsApproved(allEligibleMotorsApproved(form.getMotors(), eligibleMotors))
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .totalMotorCount(eligibleMotors.size())
                .remarks(form.getRemarks())
                .rejectionReason(form.getRejectionReason())
                .actionBy(approver.getUsername())
                .actionAt(now)
                .build();
    }

    private Map<String, Object> buildMotorBlockchainPayload(StaticTesting form, StaticMotorData motor, String approverName, LocalDateTime approvalTime) {
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
            motorMap.put("subType", motor.getSubType());
            motorMap.put("motorSubmissionType", motor.getMotorSubmissionType());
            motorMap.put("motorSubmissionStatus", motor.getMotorSubmissionStatus());
            motorMap.put("stfTestNo", motor.getStfTestNo());

            if (motor.getStaticTestingDetails() != null) {
                StfPayloads.StaticTestingDetails details = motor.getStaticTestingDetails();
                Map<String, Object> detailsMap = new LinkedHashMap<>();

                // Check and map BemStaticTestingDetails specific fields if applicable
                if (details instanceof StfPayloads.BemStaticTestingDetails bemDetails) {
                    detailsMap.put("testHeader", bemDetails.getTestHeader());
                    detailsMap.put("igniterDetails", bemDetails.getIgniterDetails());
                    detailsMap.put("nozzleDetails", bemDetails.getNozzleDetails());
                    detailsMap.put("bemData", bemDetails.getBemData());
                    detailsMap.put(
                            "ballisticParameters",
                            bemDetails.getBallisticParameters() == null
                                    ? Collections.emptyList()
                                    : bemDetails.getBallisticParameters());
                    detailsMap.put("burnRate", bemDetails.getBurnRate());
                }
                // Check and map MainMotorStaticTestingDetails specific fields if applicable
                else if (details instanceof StfPayloads.MainMotorStaticTestingDetails mainDetails) {
                    detailsMap.put("testHeader", mainDetails.getTestHeader());
                    detailsMap.put("nozzleDetails", mainDetails.getNozzleDetails());
                    detailsMap.put("mainMotorData", mainDetails.getMainMotorData());
                    detailsMap.put(
                            "ballisticParameters",
                            mainDetails.getBallisticParameters() == null
                                    ? Collections.emptyList()
                                    : mainDetails.getBallisticParameters());
                    detailsMap.put("burnRate", mainDetails.getBurnRate());
                }

                motorMap.put("staticTestingDetails", detailsMap);
            }
        }
        rootPayload.put("motorDetails", motorMap);

        return rootPayload;
    }

    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverUserId) {
        StaticTesting entity = stfRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Static Testing form not found: " + request.getFormId()));
        if (!Objects.equals(request.getSubDepartmentId(), entity.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The approval request does not belong to STF");
        }

        User approver = getUser(approverUserId);
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
        entity.setUpdatedBy(person(approver));

        stfRepo.save(entity);
//        if (request.getActionType() == Status.APPROVED) {
//            approvalBlockchainAssetService.createOnApprovedStatus(
//                    entity.getSubDepartmentId(),
//                    entity.getBatchId(),
//                    entity.getFormId(),
//                    entity,
//                    approverUserId);
//        }
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

    private List<StaticMotorData> prepareIncomingMotors(
            Batch batch,
            List<StfMotorRequest> motorForms,
            Map<String, StfMotorSubType> eligibleMotors,
            Boolean checkStfUniqueOnUpdate) {
        if (motorForms == null || motorForms.isEmpty()) {
            throw new AcemException(
                    ErrorCode.MISSING_MANDATORY_FIELD,
                    "At least one motor is required");
        }

        LinkedHashMap<String, StaticMotorData> motorsById = new LinkedHashMap<>();
        Set<String> stfTestNumbersInRequest = new HashSet<>();

        for (StfMotorRequest motorForm : motorForms) {
            if (motorForm == null) {
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Each motor is required");
            }
            String motorId = motorForm.getMotorId();
            StfMotorSubType subType = motorForm.getSubType();
            if (subType == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "subType must be MAIN_MOTOR or BEM for motor " + motorId);
            }
            StfMotorSubType expectedSubType = eligibleMotors.get(motorId);
            if (expectedSubType == null || !expectedSubType.equals(subType)) {
                throw new AcemException(
                        ErrorCode.MOTORS_NOT_FOUND,
                        "Motor " + motorId + " is not an eligible " + subType + " for this batch");
            }
            if (motorsById.containsKey(motorId)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor " + motorId + " is duplicated in the request");
            }
            mainBatchGateValidator.assertMotorUnlocked(
                    batch, MainBatchStageConstants.STF, motorId);

            // --- STF TEST NO VALIDATION ---
            String stfTestNo = motorForm.getStfTestNo();

            // 1. Check uniqueness within the incoming request payload (always runs)
            if (!stfTestNumbersInRequest.add(stfTestNo)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Duplicate stfTestNo '" + stfTestNo + "' found in the request");
            }

            // 2. Database uniqueness check (Skipped if checkStfUniqueOnUpdate is true)
            boolean skipDbCheck = Boolean.TRUE.equals(checkStfUniqueOnUpdate);

            if (!skipDbCheck) {
                if (bemMotorRepository.existsByStfTestNoAndIsDeletedFalse(stfTestNo)
                        || stfRepo.existsByMotorsStfTestNoAndIsDeletedFalse(stfTestNo)) {
                    throw new AcemException(
                            ErrorCode.MOTOR_ALREADY_EXISTS,
                            "stfTestNo '" + stfTestNo + "' is already in use.");
                }
            }

            Status submissionType = motorForm.getMotorSubmissionType();
            if (submissionType != Status.DRAFT && submissionType != Status.SUBMIT) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorSubmissionType must be DRAFT or SUBMIT for motor " + motorId);
            }

            motorsById.put(motorId, StaticMotorData.builder()
                    .motorId(motorId)
                    .subType(subType)
                    .stfTestNo(stfTestNo) // Set stfTestNo here
                    .motorSubmissionType(submissionType)
                    .motorSubmissionStatus(submissionType == Status.DRAFT
                            ? Status.IN_PROGRESS
                            : Status.WAITING_FOR_APPROVAL)
                    .staticTestingDetails(motorForm.getStaticTestingDetails())
                    .build());
        }
        return new ArrayList<>(motorsById.values());
    }

    private LinkedHashMap<String, StfMotorSubType> eligibleMotors(Batch batch) {
        LinkedHashMap<String, StfMotorSubType> motors = new LinkedHashMap<>();
        if (batch.getMotorIds() != null) {
            for (String motorId : batch.getMotorIds()) {
                if (motorId != null && !motorId.isBlank()) {
                    motors.putIfAbsent(motorId, MAIN_MOTOR);
                }
            }
        }

        SubscaleProcessing subscale = subscaleRepo
                .findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                .orElse(null);
        HardwarePreparationDetailsDTO hardware = subscale != null
                ? subscale.getHardwarePreparationDetails()
                : null;
        List<TrimmingTableDTO> trimmingTable = subscale != null
                && subscale.getTrimmingDetails() != null
                && subscale.getTrimmingDetails().getTrimmingTable() != null
                ? subscale.getTrimmingDetails().getTrimmingTable()
                : Collections.emptyList();

        for (TrimmingTableDTO trimming : trimmingTable) {
            if (trimming == null
                    || !SubscaleMetadataResolver.isBemArticleType(hardware, trimming.getArticleType())) {
                continue;
            }
            String bemMotorId = trimming.getBemNo() == null ? null : trimming.getBemNo().trim();
            if (bemMotorId == null || bemMotorId.isBlank()) {
                continue;
            }
            StfMotorSubType existingType = motors.get(bemMotorId);
            if (MAIN_MOTOR.equals(existingType)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor ID " + bemMotorId + " is configured as both MAIN_MOTOR and BEM");
            }
            motors.putIfAbsent(bemMotorId, BEM);
        }
        return motors;
    }

    private List<MotorStatusResponse> buildFullMotorStatuses(
            Batch batch,
            List<StaticMotorData> savedMotors,
            Map<String, StfMotorSubType> eligibleMotors) {
        Map<String, StaticMotorData> savedById = savedMotors == null
                ? Collections.emptyMap()
                : savedMotors.stream().collect(Collectors.toMap(
                StaticMotorData::getMotorId,
                Function.identity(),
                (first, ignored) -> first));

        List<MotorStatusResponse> statuses = new ArrayList<>(eligibleMotors.size());
        eligibleMotors.forEach((motorId, subType) -> {
            StaticMotorData motor = savedById.get(motorId);
            statuses.add(MotorStatusResponse.builder()
                    .motorId(motorId)
                    .subType(subType.name())
                    .motorSubmissionType(motor == null ? Status.DRAFT : motorSubmissionType(motor))
                    .motorSubmissionStatus(motor == null
                            ? MainBatchStageProgressHelper.defaultMotorSubmissionStatus(
                                    batch, MainBatchStageConstants.STF, motorId)
                            : motorStatus(motor))
                    .build());
        });
        return BatchMotorDiscardHelper.overlayMotorStatuses(batch, statuses);
    }

    private Status calculateFormStatus(List<StaticMotorData> motors, int totalRequiredMotors) {
        if (motors == null || motors.isEmpty()) {
            return Status.TO_BE_INITIATED;
        }
        if (motors.stream().anyMatch(motor -> motorStatus(motor) == Status.REJECTED)) {
            return Status.REJECTED;
        }

        long waitingCount = motors.stream()
                .filter(motor -> motorStatus(motor) == Status.WAITING_FOR_APPROVAL)
                .count();
        long approvedCount = motors.stream()
                .filter(motor -> motorStatus(motor) == Status.APPROVED)
                .count();
        if (totalRequiredMotors > 0
                && approvedCount == totalRequiredMotors && waitingCount == 0) {
            return Status.COMPLETELY_APPROVED;
        }
        if (waitingCount > 0) {
            if (totalRequiredMotors == 1 || (totalRequiredMotors <= 0 && motors.size() == 1)) {
                return Status.WAITING_FOR_APPROVAL;
            }
            return Status.WAITING_FOR_PARTIAL_APPROVAL;
        }
        if (approvedCount > 0) {
            return Status.IN_PROGRESS;
        }
        return Status.IN_PROGRESS;
    }

    private MotorCounts countMotors(List<StaticMotorData> motors) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int inProgress = 0;
        for (StaticMotorData motor : motors == null ? Collections.<StaticMotorData>emptyList() : motors) {
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

    private boolean allEligibleMotorsApproved(
            List<StaticMotorData> motors,
            Map<String, StfMotorSubType> eligibleMotors) {
        if (eligibleMotors.isEmpty()) {
            return true;
        }
        if (motors == null) {
            return false;
        }
        Map<String, StaticMotorData> savedById = motors.stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .collect(Collectors.toMap(
                        StaticMotorData::getMotorId,
                        Function.identity(),
                        (first, ignored) -> first));
        return eligibleMotors.entrySet().stream().allMatch(entry -> {
            StaticMotorData motor = savedById.get(entry.getKey());
            return motor != null
                    && entry.getValue().equals(motor.getSubType())
                    && motorStatus(motor) == Status.APPROVED;
        });
    }

    private StfUpdateResponse buildUpdateResponse(
            StaticTesting form,
            List<StaticMotorData> motors,
            Map<String, StfMotorSubType> eligibleMotors,
            List<MotorStatusResponse> motorStatuses,
            MotorCounts counts,
            String userId,
            LocalDateTime now,
            boolean finalSubmit) {
        StfUpdateResponse.StfUpdateResponseBuilder response = StfUpdateResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .formStatus(form.getStatus())
                .motorCount(motors.size())
                .motors(motorIds(motors))
                .updatedBy(userId)
                .updatedOn(now)
                .allMotorsApproved(allEligibleMotorsApproved(motors, eligibleMotors))
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .inProgressMotorCount(counts.inProgress())
                .totalMotorCount(eligibleMotors.size())
                .motorStatuses(motorStatuses);
        if (finalSubmit) {
            response.submittedBy(userId)
                    .submittedOn(now);
        }
        return response.build();
    }

    private List<MotorIdDto> motorIds(List<StaticMotorData> motors) {
        return motors.stream()
                .map(motor -> MotorIdDto.builder()
                        .motorId(motor.getMotorId())
                        .subType(motor.getSubType())
                        .build())
                .toList();
    }

    private boolean hasSubmittedMotor(List<StaticMotorData> motors) {
        return motors != null && motors.stream()
                .filter(Objects::nonNull)
                .anyMatch(motor -> motor.getMotorSubmissionType() == Status.SUBMIT);
    }

    private void syncStage(
            Batch batch,
            StaticTesting form,
            Status status,
            List<MotorStatusResponse> motorStatuses,
            PersonInfo submittedBy,
            LocalDateTime now,
            boolean approvedUnitEvent,
            String approverUserId) {
        if (approverUserId != null) {
            batchService.syncStageApproval(
                    batch.getBatchId(),
                    form.getSubDepartmentId(),
                    status,
                    null,
                    null,
                    motorStatuses,
                    approvedUnitEvent,
                    form.getRemarks(),
                    form.getRejectionReason(),
                    approverUserId,
                    now);
        } else {
            batchService.syncStageProgress(
                    batch.getBatchId(),
                    form.getSubDepartmentId(),
                    status,
                    null,
                    null,
                    motorStatuses,
                    submittedBy,
                    now,
                    approvedUnitEvent,
                    form.getRemarks(),
                    form.getRejectionReason());
        }
    }

    private void validateMotorApprovalRequest(MotorApprovalRequest request) {
        if (request.getActionType() != Status.APPROVED
                && request.getActionType() != Status.REJECTED) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "actionType must be APPROVED or REJECTED");
        }
        if (request.getActionType() == Status.REJECTED
                && (request.getRejectionReason() == null
                || request.getRejectionReason().isBlank())) {
            throw new AcemException(
                    ErrorCode.REJECTION_REASON_REQUIRED,
                    "rejectionReason is required for rejected motor");
        }
    }

    private void validateFormSubmissionType(Status status) {
        if (status != Status.DRAFT && status != Status.SUBMIT) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "formSubmissionType must be DRAFT or SUBMIT");
        }
    }

    private boolean isProtectedMotor(StaticMotorData motor) {
        Status status = motorStatus(motor);
        return status == Status.APPROVED || status == Status.WAITING_FOR_APPROVAL;
    }

    private Status motorSubmissionType(StaticMotorData motor) {
        return motor.getMotorSubmissionType() != null ? motor.getMotorSubmissionType() : Status.DRAFT;
    }

    private Status motorStatus(StaticMotorData motor) {
        return motor.getMotorSubmissionStatus() != null
                ? motor.getMotorSubmissionStatus()
                : Status.IN_PROGRESS;
    }

    private List<StaticMotorData> safeMotors(StaticTesting form) {
        return form.getMotors() != null ? form.getMotors() : Collections.emptyList();
    }

    private Batch getBatch(String batchId) {
        return batchRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId " + batchId));
    }

    public User getUser(String userId) {
        return userRepo.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    private void validateCurrentStage(Batch batch, Long subDepartmentId) {
        batchService.assertStageUnlockedAndActive(batch, subDepartmentId, "Static Test Facility");
    }

    protected PersonInfo person(User user) {
        return PersonInfo.builder()
                .id(user.getUserId())
                .fullName(user.getUsername())
                .build();
    }

    private record MotorCounts(int pending, int approved, int rejected, int inProgress) {
    }
}
