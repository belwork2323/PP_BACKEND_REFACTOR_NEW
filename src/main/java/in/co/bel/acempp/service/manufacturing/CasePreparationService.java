package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.*;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.manufacturing.CasePreparation;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.manufacturing.CasePreparationRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.service.BatchService;
import in.co.bel.acempp.service.batch.BatchMotorDiscardHelper;
import in.co.bel.acempp.service.batch.MainBatchStageConstants;
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
import java.util.Collections;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasePreparationService {

    private final CasePreparationRepository casePrepRepo;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final MaterialRepository materialRepository;
    private final BatchService batchService;
    private final FormIdGeneratorService formIdGenerator;
    private final CasePreparationDtoMapper casePreparationDtoMapper;
    private final CasePreparationAbradingMapper casePreparationAbradingMapper;
    private final FileBindingService fileBindingService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;

    //------------------------------------CREATE FORM-------------------------------------------------------
    public CasePreparationResponse createForm(CreateCasePreparationRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());

        // 1. Validate batch
        Batch batch = batchRepo.findByBatchId(request.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId " + request.getBatchId()));

        if (batch.isDeleted()) {
            throw new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch is deleted");
        }
        batchService.assertStageUnlockedAndActive(batch, request.getSubDepartmentId(), "Case Preparation");

        // 2. Check duplicate form
        if (casePrepRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Case preparation form already submitted for this batch");
        }

        // 3. Validate motors
        if (request.getCasePreparationDetails() == null
                || request.getCasePreparationDetails().getMotors() == null
                || request.getCasePreparationDetails().getMotors().isEmpty()) {

            throw new AcemException(ErrorCode.MISSING_MANDATORY_FIELD, "At least one motor is required");
        }

        List<CasePreparationMotorRequest> requestedMotors = request.getCasePreparationDetails().getMotors();

        for (CasePreparationMotorRequest motor : requestedMotors) {
            if (!batch.getMotorIds().contains(motor.getMotorId())) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor " + motor.getMotorId() + " does not belong to batch.");
            }
            BatchMotorDiscardHelper.assertNotDiscarded(batch, motor.getMotorId());
            resolveMotorSubmissionStatus(motor.getMotorSubmissionType());
        }

        // 4. Generate Form Id
        String formId = formIdGenerator.generate("case_preparations", "formId", "CPF", 3);

        // 5. File Promotion for Create
        List<DocumentDto> promotedFiles = new ArrayList<>();
        try {
            promotedFiles = promoteCasePrepFiles(
                    requestedMotors,
                    request.getSubDepartmentId(),
                    formId,
                    Collections.emptySet() // No existing files during creation
            );

            CasePreparationDetailsRequest persistedDetails = casePreparationDtoMapper.toPersistence(request.getCasePreparationDetails());
            List<CasePreparationMotorRequest> motors = persistedDetails.getMotors();
            motors.forEach(motor -> motor.setMotorSubmissionStatus(
                    resolveMotorSubmissionStatus(motor.getMotorSubmissionType())));

            int totalRequiredMotors = batch.getNumberOfMotors();
            Status batchStatus = calculateBatchStatus(motors, totalRequiredMotors);

            User user = userRepo.findByUserId(userId)
                    .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));

            LocalDateTime now = LocalDateTime.now();

            // 6. Build & Save CasePreparation Entity
            CasePreparation entity = CasePreparation.builder()
                    .formId(formId)
                    .batchId(request.getBatchId())
                    .subDepartmentId(request.getSubDepartmentId())
                    .formSubmissionType(request.getFormSubmissionType())
                    .status(batchStatus)
                    .casePreparationDetails(persistedDetails)
                    .submittedBy(PersonInfo.builder()
                            .id(userId)
                            .fullName(user.getUsername())
                            .build())
                    .submittedAt(now)
                    .build();

            entity = casePrepRepo.save(entity);
            log.info("Case preparation form {} created for batch {}", formId, request.getBatchId());

            // 7. Sync Progress & Build Response
            List<MotorStatusResponse> motorStatuses = buildFullMotorStatuses(batch, motors, batch.getMotorIds());
            batchService.syncStageProgress(
                    batch.getBatchId(),
                    request.getSubDepartmentId(),
                    batchStatus,
                    null,
                    null,
                    motorStatuses,
                    (request.getFormSubmissionType() == Status.SUBMIT || hasSubmittedMotor(motors))
                            ? PersonInfo.builder().id(userId).fullName(user.getUsername()).build()
                            : null,
                    now,
                    false,
                    null,
                    null);

            int pending = (int) motors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
            int approved = (int) motors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.APPROVED).count();
            int rejected = (int) motors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.REJECTED).count();
            int inProgress = (int) motors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.IN_PROGRESS).count();

            return CasePreparationResponse.builder()
                    .formId(entity.getFormId())
                    .batchId(entity.getBatchId())
                    .subDepartmentId(entity.getSubDepartmentId())
                    .formSubmissionType(entity.getFormSubmissionType())
                    .status(batchStatus)
                    .submittedBy(PersonInfo.builder()
                            .id(user.getUserId())
                            .fullName(user.getUsername())
                            .build())
                    .submittedAt(now)
                    .pendingMotorCount(pending)
                    .approvedMotorCount(approved)
                    .rejectedMotorCount(rejected)
                    .inProgressMotorCount(inProgress)
                    .totalMotorCount(totalRequiredMotors)
                    .motorStatuses(motorStatuses)
                    .casePreparationDetails(normalizeCasePreparationDetails(entity.getCasePreparationDetails()))
                    .build();

        } catch (Exception ex) {
            log.error("Failed to create Case Preparation form. Triggering file rollback...", ex);
            rollbackPromotedFiles(promotedFiles);
            throw ex;
        }
    }

    public CasePreparationResponse updateForm(CreateCasePreparationRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());

        // 1. Fetch existing form & batch
        CasePreparation form = casePrepRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Case preparation form not found"));

        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(form.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "Batch not found"));

        batchService.assertStageUnlockedAndActive(batch, request.getSubDepartmentId(), "Case Preparation");

        if (form.getStatus() == Status.APPROVED) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Approved form cannot be modified");
        }

        if (request.getCasePreparationDetails() == null
                || request.getCasePreparationDetails().getMotors() == null
                || request.getCasePreparationDetails().getMotors().isEmpty()) {

            throw new AcemException(ErrorCode.MISSING_MANDATORY_FIELD, "At least one motor is required");
        }

        int totalRequiredMotors = batch.getNumberOfMotors();
        boolean isFinalFormSubmit = (request.getFormSubmissionType() == Status.SUBMIT);

        // Map existing motors by Motor ID
        Map<String, CasePreparationMotorRequest> existingMotorMap =
                form.getCasePreparationDetails() != null && form.getCasePreparationDetails().getMotors() != null
                        ? new HashMap<>(form.getCasePreparationDetails().getMotors().stream()
                        .collect(Collectors.toMap(CasePreparationMotorRequest::getMotorId, Function.identity())))
                        : new HashMap<>();

        // 2. Validate modification rules & batch membership
        if (!isFinalFormSubmit) {
            for (CasePreparationMotorRequest motorRequest : request.getCasePreparationDetails().getMotors()) {
                if (!batch.getMotorIds().contains(motorRequest.getMotorId())) {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "Motor " + motorRequest.getMotorId() + " does not belong to batch.");
                }
                BatchMotorDiscardHelper.assertNotDiscarded(batch, motorRequest.getMotorId());

                CasePreparationMotorRequest existing = existingMotorMap.get(motorRequest.getMotorId());
                if (existing != null) {
                    Status currentStatus = existing.getMotorSubmissionStatus();
                    if (currentStatus == Status.WAITING_FOR_APPROVAL || currentStatus == Status.APPROVED) {
                        throw new AcemException(
                                ErrorCode.FORM_ALREADY_EXISTS,
                                "Motor " + motorRequest.getMotorId() + " cannot be modified.");
                    }
                }
            }
        }

        // 3. FILE LIFECYCLE MANAGEMENT (PURGE & PROMOTE / ENRICH)
        // FE often saves one motor at a time; merge with locked motors before computing removals
        // so approved/waiting motors' files are not treated as deleted.
        List<CasePreparationMotorRequest> incomingMotors = request.getCasePreparationDetails().getMotors();

        for (CasePreparationMotorRequest motorRequest : incomingMotors) {
            CasePreparationMotorRequest existing = existingMotorMap.get(motorRequest.getMotorId());

            if (existing != null &&
                    (existing.getMotorSubmissionStatus() == Status.APPROVED ||
                            existing.getMotorSubmissionStatus() == Status.WAITING_FOR_APPROVAL)) {
                existingMotorMap.put(existing.getMotorId(), existing);
            } else {
                Status newStatus = resolveMotorSubmissionStatus(motorRequest.getMotorSubmissionType());
                CasePreparationMotorRequest persistedMotor = casePreparationDtoMapper.toPersistence(motorRequest);
                persistedMotor.setMotorSubmissionStatus(newStatus);
                existingMotorMap.put(persistedMotor.getMotorId(), persistedMotor);
            }
        }

        List<CasePreparationMotorRequest> updatedMotors = new ArrayList<>(existingMotorMap.values());

        // A. Target file set = files on the merged form (all motors), not just the partial payload
        Set<String> targetFileIds = extractMotorListFileIds(updatedMotors);

        // B. Fetch existing bound files for this form from MongoDB
        List<FileAttachments> existingAttachments = fileBindingService.getAttachmentsByFormId(form.getFormId());
        Set<String> existingFileIds = existingAttachments.stream()
                .map(FileAttachments::getFileId)
                .collect(Collectors.toSet());

        // C. Purge only files truly removed from editable motors
        List<String> removedFileIds = existingFileIds.stream()
                .filter(fileId -> !targetFileIds.contains(fileId))
                .collect(Collectors.toList());

        if (!removedFileIds.isEmpty()) {
            fileBindingService.purgeRemovedFilesFromDiskAndDb(form.getFormId(), removedFileIds);
        }

        // D. Promote newly uploaded files with transaction rollback guard (Enriches existing files)
        List<DocumentDto> newlyPromotedFiles = new ArrayList<>();
        try {
            newlyPromotedFiles = promoteCasePrepFiles(
                    incomingMotors,
                    request.getSubDepartmentId(),
                    form.getFormId(),
                    existingFileIds
            );

            // 5. Calculate dynamic batch status
            Status batchStatus = calculateBatchStatus(updatedMotors, totalRequiredMotors);
            form.setSubDepartmentId(request.getSubDepartmentId());
            form.setFormSubmissionType(request.getFormSubmissionType());
            form.setStatus(batchStatus);

            CasePreparationDetailsRequest details = form.getCasePreparationDetails();
            if (details == null) {
                details = new CasePreparationDetailsRequest();
            }
            details.setMotors(updatedMotors);
            form.setCasePreparationDetails(details);

            User user = userRepo.findByUserId(userId)
                    .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));

            LocalDateTime now = LocalDateTime.now();

            form.setUpdatedBy(PersonInfo.builder()
                    .id(userId)
                    .fullName(user.getUsername())
                    .build());
            form.setUpdatedAt(now);

            if (isFinalFormSubmit) {
                form.setSubmittedBy(PersonInfo.builder()
                        .id(userId)
                        .fullName(user.getUsername())
                        .build());
                form.setSubmittedAt(now);
            }

            CasePreparation savedForm = casePrepRepo.save(form);

            // 7. Sync Stage Progress & Build Response
            List<MotorStatusResponse> motorStatuses = buildFullMotorStatuses(batch, updatedMotors, batch.getMotorIds());
            batchService.syncStageProgress(
                    batch.getBatchId(),
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    null,
                    null,
                    motorStatuses,
                    isFinalFormSubmit || hasSubmittedMotor(updatedMotors)
                            ? PersonInfo.builder().id(userId).fullName(user.getUsername()).build()
                            : null,
                    now,
                    false,
                    form.getRemarks(),
                    form.getRejectionReason());

            long pending = updatedMotors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
            long approved = updatedMotors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.APPROVED).count();
            long rejected = updatedMotors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.REJECTED).count();
            long inProgress = updatedMotors.stream().filter(m -> m.getMotorSubmissionStatus() == Status.IN_PROGRESS).count();
            LocalDateTime submittedAt =
                    savedForm.getSubmittedAt() != null ? savedForm.getSubmittedAt() : now;

            return CasePreparationResponse.builder()
                    .formId(savedForm.getFormId())
                    .batchId(savedForm.getBatchId())
                    .subDepartmentId(savedForm.getSubDepartmentId())
                    .formSubmissionType(savedForm.getFormSubmissionType())
                    .status(batchStatus)
                    .submittedBy(PersonInfo.builder()
                            .id(user.getUserId())
                            .fullName(user.getUsername())
                            .build())
                    .submittedAt(submittedAt)
                    .updatedBy(userId)
                    .updatedAt(now)
                    .pendingMotorCount((int) pending)
                    .approvedMotorCount((int) approved)
                    .rejectedMotorCount((int) rejected)
                    .inProgressMotorCount((int) inProgress)
                    .totalMotorCount(totalRequiredMotors)
                    .motorStatuses(motorStatuses)
                    .casePreparationDetails(normalizeCasePreparationDetails(savedForm.getCasePreparationDetails()))
                    .build();

        } catch (Exception ex) {
            log.error("Failed to update Case Preparation form. Triggering file rollback...", ex);
            rollbackPromotedFiles(newlyPromotedFiles);
            throw ex;
        }
    }

    //------------------------------------FORM DETAILS-------------------------------------------------------

    public CasePreparationResponse getFormDetails(String formId) {

        CasePreparation form = casePrepRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "No case preparation form exists for formId " + formId));

        Batch batch = batchRepo.findByBatchId(form.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "Batch not found"));

        List<CasePreparationMotorRequest> motors = form.getCasePreparationDetails() != null
                ? form.getCasePreparationDetails().getMotors()
                : Collections.emptyList();

        long pending = motors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.WAITING_FOR_APPROVAL)
                .count();

        long approved = motors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.APPROVED)
                .count();

        long rejected = motors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.REJECTED)
                .count();

        long inProgress = motors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.IN_PROGRESS)
                .count();

//        List<Motor> motors = entity.getCasePreparationDetails().getMotors();
        List<MotorStatusResponse> motorStatuses =
                buildFullMotorStatuses(batch, motors, batch.getMotorIds());

        CasePreparationDetailsRequest details = normalizeCasePreparationDetails(form.getCasePreparationDetails());
        enrichCasePrepMotorFiles(details, form.getSubDepartmentId());
        List<String> discardedMotorIds = BatchMotorDiscardHelper.discardedMotorIds(batch);
        if (details != null && details.getMotors() != null) {
            for (CasePreparationMotorRequest m : details.getMotors()) {
                if (m != null && BatchMotorDiscardHelper.isDiscarded(batch, m.getMotorId())) {
                    m.setMotorSubmissionStatus(Status.DISCARDED);
                }
            }
        }

        return CasePreparationResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .subDepartmentId(form.getSubDepartmentId())
                .formSubmissionType(form.getFormSubmissionType())
                .status(form.getStatus())

                .allMotorsApproved(approved == batch.getNumberOfMotors())
                .pendingMotorCount((int) pending)
                .approvedMotorCount((int) approved)
                .rejectedMotorCount((int) rejected)
                .inProgressMotorCount((int) inProgress)
                .totalMotorCount(batch.getNumberOfMotors())

                .motorStatuses(motorStatuses)
                .discardedMotorIds(discardedMotorIds)

                .createdBy(batch.getCreatedBy() != null
                        ? batch.getCreatedBy().getId()
                        : null)
                .createdAt(batch.getCreatedAt())

                .submittedBy(form.getSubmittedBy())
                .submittedAt(form.getSubmittedAt())

                .casePreparationDetails(details)
                .build();
    }


    @Transactional(rollbackFor = Exception.class)
    public MotorApprovalResponse changeStatusOfMotor(MotorApprovalRequest request, String approverName) {

        if (request.getActionType() == Status.REJECTED &&
                (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {

            throw new AcemException(
                    ErrorCode.REJECTION_REASON_REQUIRED,
                    "rejectionReason is required for rejected motor.");
        }

        CasePreparation form = casePrepRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Case preparation form not found"));

        CasePreparationMotorRequest motor = form.getCasePreparationDetails()
                .getMotors()
                .stream()
                .filter(m -> m.getMotorId().equals(request.getMotorId()))
                .findFirst()
                .orElseThrow(() -> new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Motor not found"));

        Batch batch = batchRepo.findByBatchId(form.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "Batch not found"));
        BatchMotorDiscardHelper.assertNotDiscarded(batch, request.getMotorId());

        if (motor.getMotorSubmissionStatus() != Status.WAITING_FOR_APPROVAL) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Motor is not waiting for approval.");
        }

        LocalDateTime now = LocalDateTime.now();
        motor.setMotorSubmissionStatus(request.getActionType());
        form.setUpdatedBy(
                PersonInfo.builder()
                        .fullName(approverName)
                        .build());

        form.setUpdatedAt(now);
        form.setRemarks(request.getRemarks());
        form.setRejectionReason(request.getRejectionReason());

        int totalRequiredMotors = batch.getNumberOfMotors();
        List<CasePreparationMotorRequest> motors = form.getCasePreparationDetails().getMotors();

        // 3. Recalculate Batch/Form level status dynamically
        Status batchStatus = calculateBatchStatus(motors, totalRequiredMotors);
        form.setStatus(batchStatus);
        if (batchStatus == Status.COMPLETELY_APPROVED) {
            form.setFormSubmissionType(Status.SUBMIT);
            form.setSubmittedBy(form.getUpdatedBy());
            form.setSubmittedAt(now);
            form.setRejectionReason(null);
        }

        if (request.getActionType() == Status.APPROVED) {
            fileReadOnlyService.markReadOnly(form.getSubDepartmentId(), extractMotorFileIds(motor));
        }
        if (batchStatus == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }

        if (request.getActionType() == Status.APPROVED) {
            try {
                Map<String, Object> blockchainPayload = buildMotorBlockchainPayload(form ,  motor);
                approvalBlockchainAssetService.createAssetOnBlockchain(
                        form.getSubDepartmentId(),
                        form.getBatchId(),
                        form.getFormId(),
                        blockchainPayload,
                        request.getMotorId(),
                        approverName);
            } catch (Exception e) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
            }
        }

        casePrepRepo.save(form);

        List<MotorStatusResponse> motorStatuses =
                buildFullMotorStatuses(batch, motors, batch.getMotorIds());
        batchService.syncStageApproval(
                form.getBatchId(),
                form.getSubDepartmentId(),
                batchStatus,
                null,
                null,
                motorStatuses,
                request.getActionType() == Status.APPROVED,
                form.getRemarks(),
                form.getRejectionReason(),
                approverName,
                now);
        long pending = motors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.WAITING_FOR_APPROVAL)
                .count();

        long approved = motors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.APPROVED)
                .count();

        long rejected = motors.stream()
                .filter(m -> m.getMotorSubmissionStatus() == Status.REJECTED)
                .count();

        return MotorApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .motorId(request.getMotorId())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .motorSubmissionStatus(motor.getMotorSubmissionStatus())
                .batchStatus(batchStatus)
                .allMotorsApproved(approved == totalRequiredMotors)
                .pendingMotorCount((int) pending)
                .approvedMotorCount((int) approved)
                .rejectedMotorCount((int) rejected)
                .totalMotorCount(totalRequiredMotors)
                .remarks(form.getRemarks())
                .rejectionReason(form.getRejectionReason())
                .actionBy(approverName)
                .actionAt(now)
                .build();
    }

    private Map<String, Object> buildMotorBlockchainPayload(CasePreparation form, CasePreparationMotorRequest motor) {
        Map<String, Object> rootPayload = new LinkedHashMap<>();

        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getSubmittedBy() != null) {
                auditMap.put("createdBy", form.getSubmittedBy().getFullName());
            }
            if (form.getSubmittedAt() != null) {
                auditMap.put("createdAt", form.getSubmittedAt());
            }
            if (form.getUpdatedBy() != null) {
                auditMap.put("approvedBy", form.getUpdatedBy().getFullName());
            }
            if (form.getUpdatedAt() != null) {
                auditMap.put("approvedAt", form.getUpdatedAt());
            }
        }
        rootPayload.put("audit", auditMap);

        Map<String, Object> motorMap = new LinkedHashMap<>();
        if (motor != null) {
            motorMap.put("motorId", motor.getMotorId());
            if (motor.getPrrcClearanceDate() != null) {
                motorMap.put("prrcClearanceDate", motor.getPrrcClearanceDate().toString());
            }

            if (motor.getAbradingOperation() != null) {
                var abrading = motor.getAbradingOperation();
                Map<String, Object> abradingMap = new LinkedHashMap<>();
                abradingMap.put("typeOfCasing", abrading.getTypeOfCasing());
                abradingMap.put("typeOfInsulation", abrading.getTypeOfInsulation());
                abradingMap.put("abradingWheelType", abrading.getAbradingWheelType());
                abradingMap.put("firstCut", abrading.getFirstCut() == null ? Collections.emptyList() : abrading.getFirstCut());
                abradingMap.put("secondCut", abrading.getSecondCut() == null ? Collections.emptyList() : abrading.getSecondCut());
                motorMap.put("abradingOperation", abradingMap);
            }

            if (motor.getBellowBonding() != null) {
                var bellow = motor.getBellowBonding();
                Map<String, Object> bellowMap = new LinkedHashMap<>();
                bellowMap.put("heBellowDimension", bellow.getHeBellowDimension());
                bellowMap.put("neBellowDimension", bellow.getNeBellowDimension());
                bellowMap.put("numberOfSpacers", bellow.getNumberOfSpacers());
                motorMap.put("bellowBonding", bellowMap);
            }

            if (motor.getTceCleaning() != null) {
                var tce = motor.getTceCleaning();
                Map<String, Object> tceMap = new LinkedHashMap<>();
                tceMap.put("solventUsedQtyKg", tce.getSolventUsedQtyKg());
                tceMap.put("observation", tce.getObservation());
                motorMap.put("tceCleaning", tceMap);
            }

            if (motor.getPreHeating() != null) {
                var preHeat = motor.getPreHeating();
                Map<String, Object> preHeatMap = new LinkedHashMap<>();
                preHeatMap.put("vacuumBaggingApplied", preHeat.getVacuumBaggingApplied());
                preHeatMap.put("preHeatingRecipe", preHeat.getPreHeatingRecipe());
                motorMap.put("preHeating", preHeatMap);
            }

            if (motor.getLinerCoatingOperation() != null) {
                var liner = motor.getLinerCoatingOperation();
                Map<String, Object> linerMap = new LinkedHashMap<>();
                linerMap.put("linerType", liner.getLinerType());
                linerMap.put("batchNo", liner.getBatchNo());
                linerMap.put("qualifyingSubscaleBatchNo", liner.getQualifyingSubscaleBatchNo());
                motorMap.put("linerCoatingOperation", linerMap);
            }

            if (motor.getDispatchToCasting() != null) {
                motorMap.put("dispatchToCasting", motor.getDispatchToCasting());
            }
        }
        rootPayload.put("motorData", motorMap);

        return rootPayload;
    }

    private List<MotorStatusResponse> buildFullMotorStatuses(
            Batch batch,
            List<CasePreparationMotorRequest> savedMotors,
            List<String> batchMotorIds) {

        List<MotorStatusResponse> responseList = new ArrayList<>();

        Map<String, CasePreparationMotorRequest> savedMap = savedMotors != null
                ? savedMotors.stream()
                .collect(Collectors.toMap(CasePreparationMotorRequest::getMotorId, Function.identity()))
                : Collections.emptyMap();

        for (String motorId : batchMotorIds) {

            if (savedMap.containsKey(motorId)) {

                CasePreparationMotorRequest motor = savedMap.get(motorId);

                responseList.add(
                        MotorStatusResponse.builder()
                                .motorId(motor.getMotorId())
                                .motorSubmissionType(motor.getMotorSubmissionType())
                                .motorSubmissionStatus(motor.getMotorSubmissionStatus())
                                .build()
                );

            } else {

                responseList.add(
                        MotorStatusResponse.builder()
                                .motorId(motorId)
                                .motorSubmissionType(Status.DRAFT)
                                .motorSubmissionStatus(MainBatchStageProgressHelper.defaultMotorSubmissionStatus(
                                        batch, MainBatchStageConstants.CASE_PREP, motorId))
                                .build()
                );
            }
        }

        return BatchMotorDiscardHelper.overlayMotorStatuses(batch, responseList);
    }

    private Status calculateBatchStatus(List<CasePreparationMotorRequest> savedMotors, int totalRequiredMotors) {

        if (savedMotors == null || savedMotors.isEmpty()) {
            return Status.TO_BE_INITIATED;
        }

        // 1. Any rejected motor fails the form status immediately
        boolean anyRejected = savedMotors.stream()
                .anyMatch(m -> m.getMotorSubmissionStatus() == Status.REJECTED);

        if (anyRejected) {
            return Status.REJECTED;
        }

        // Counts for specific states
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
                        || m.getMotorSubmissionType() == Status.DRAFT);
        if (anyStarted) {
            return Status.IN_PROGRESS;
        }

        return Status.TO_BE_INITIATED;
    }

    private boolean hasSubmittedMotor(List<CasePreparationMotorRequest> motors) {
        return motors != null && motors.stream()
                .filter(Objects::nonNull)
                .anyMatch(motor -> motor.getMotorSubmissionType() == Status.SUBMIT);
    }

    private Status resolveMotorSubmissionStatus(Status submissionType) {
        if (submissionType == Status.DRAFT) {
            return Status.IN_PROGRESS;
        }
        if (submissionType == Status.SUBMIT) {
            return Status.WAITING_FOR_APPROVAL;
        }
        throw new AcemException(
                ErrorCode.INVALID_REQUEST,
                "Invalid motorSubmissionType");
    }

    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {

        CasePreparation entity = casePrepRepo
                .findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Case preparation form not found: "
                                + request.getFormId()));

        ApprovalStatusUtil.requireLegacyFinalApprovalState(entity.getStatus());
        ApprovalStatusUtil.applyStatusChange(request.getActionType(), request.getSubDepartmentId(), request.getRemarks(), request.getRejectionReason(),
                approverName, status -> entity.setStatus(status),
                entity::setRemarks, entity::setRejectionReason, entity::setUpdatedBy, entity::setUpdatedAt
        );

        if (request.getActionType() == Status.APPROVED
                || request.getActionType() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(entity.getFormId(), entity.getSubDepartmentId());
        }

        casePrepRepo.save(entity);
//        if (request.getActionType() == Status.APPROVED) {
//            approvalBlockchainAssetService.createOnApprovedStatus(
//                    entity.getSubDepartmentId(),
//                    entity.getBatchId(),
//                    entity.getFormId(),
//                    entity,
//                    request.get
//                    approverName);
//        }
        batchService.updateBatchStatus(entity.getBatchId(), entity.getSubDepartmentId(), entity.getStatus(),
                entity.getRemarks(), entity.getRejectionReason(), approverName);
        return ApproverStatusResponse.builder()
                .formId(entity.getFormId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionAt(entity.getUpdatedAt())
                .actionBy(entity.getUpdatedBy())
                .build();
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

    private CasePreparationDetailsRequest normalizeCasePreparationDetails(CasePreparationDetailsRequest details) {
        if (details == null || details.getMotors() == null) {
            return details;
        }
        for (CasePreparationMotorRequest motor : details.getMotors()) {
            if (motor != null && motor.getAbradingOperation() != null) {
                casePreparationAbradingMapper.normalize(motor.getAbradingOperation());
            }
        }
        return details;
    }

    private void extractFileIdsFromCutRows(
            List<CasePreparationSectionPayloads.AbradingCutRow> rows,
            Set<String> fileIds) {
        if (rows == null) {
            return;
        }
        for (CasePreparationSectionPayloads.AbradingCutRow row : rows) {
            if (row != null) {
                extractFileIdsFromDocs(row.getAttachments(), fileIds);
            }
        }
    }

    private List<DocumentDto> promoteCutRowAttachments(
            List<CasePreparationSectionPayloads.AbradingCutRow> rows,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds,
            String motorId) {
        List<DocumentDto> promoted = new ArrayList<>();
        if (rows == null) {
            return promoted;
        }
        for (CasePreparationSectionPayloads.AbradingCutRow row : rows) {
            if (row != null && row.getAttachments() != null) {
                promoted.addAll(promoteDocumentList(row.getAttachments(), subDeptId, formId, existingFileIds, motorId));
            }
        }
        return promoted;
    }

    /**
     * Promotes all newly uploaded temporary files present in the request to permanent storage.
     */
    private List<DocumentDto> promoteCasePrepFiles(
            List<CasePreparationMotorRequest> motors,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> newlyPromotedFiles = new ArrayList<>();
        if (motors == null || motors.isEmpty()) {
            return newlyPromotedFiles;
        }

        for (CasePreparationMotorRequest motor : motors) {
            if (motor == null) {
                continue;
            }

            // 1. Abrading Operation Attachments (firstCut / secondCut)
            if (motor.getAbradingOperation() != null) {
                newlyPromotedFiles.addAll(promoteCutRowAttachments(
                        motor.getAbradingOperation().getFirstCut(), subDeptId, formId, existingFileIds, motor.getMotorId()));
                newlyPromotedFiles.addAll(promoteCutRowAttachments(
                        motor.getAbradingOperation().getSecondCut(), subDeptId, formId, existingFileIds, motor.getMotorId()));
            }

            // 2. TCE Cleaning Test Report (Single Document)
            if (motor.getTceCleaning() != null && motor.getTceCleaning().getTestReport() != null) {
                DocumentDto newlyPromoted = promoteDocument(
                        motor.getTceCleaning().getTestReport(), subDeptId, formId, existingFileIds, motor.getMotorId());
                if (newlyPromoted != null) {
                    newlyPromotedFiles.add(newlyPromoted);
                }
            }
        }
        return newlyPromotedFiles;
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

            // SCENARIO A: File already bound to this form -> Enrich metadata without re-promoting
            if (existingFileIds.contains(fileId)) {
                fileMetadataEnrichmentService.enrichDocument(doc, subDeptId);
                return null; // Return null so it is NOT added to newlyPromotedFiles (protects against rollback)
            }

            // SCENARIO B: New Temp File -> Promote & Bind
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
                return doc; // Return enriched doc for tracking in newlyPromotedFiles list

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

    private Set<String> extractMotorListFileIds(List<CasePreparationMotorRequest> motors) {
        Set<String> fileIds = new HashSet<>();
        if (motors == null || motors.isEmpty()) {
            return fileIds;
        }
        for (CasePreparationMotorRequest motor : motors) {
            fileIds.addAll(extractMotorFileIds(motor));
        }
        return fileIds;
    }

    private Set<String> extractMotorFileIds(CasePreparationMotorRequest motor) {
        Set<String> fileIds = new HashSet<>();
        if (motor == null) {
            return fileIds;
        }
        if (motor.getAbradingOperation() != null) {
            extractFileIdsFromCutRows(motor.getAbradingOperation().getFirstCut(), fileIds);
            extractFileIdsFromCutRows(motor.getAbradingOperation().getSecondCut(), fileIds);
        }
        if (motor.getTceCleaning() != null) {
            FileIdExtractionUtil.extractFromDocument(motor.getTceCleaning().getTestReport(), fileIds);
        }
        return fileIds;
    }

    private void enrichCasePrepMotorFiles(CasePreparationDetailsRequest details, Long subDeptId) {
        if (details == null || details.getMotors() == null) {
            return;
        }
        for (CasePreparationMotorRequest motor : details.getMotors()) {
            if (motor == null) {
                continue;
            }
            if (motor.getAbradingOperation() != null) {
                enrichCutRowAttachments(motor.getAbradingOperation().getFirstCut(), subDeptId);
                enrichCutRowAttachments(motor.getAbradingOperation().getSecondCut(), subDeptId);
            }
            if (motor.getTceCleaning() != null && motor.getTceCleaning().getTestReport() != null) {
                fileMetadataEnrichmentService.enrichDocument(motor.getTceCleaning().getTestReport(), subDeptId);
            }
        }
    }

    private void enrichCutRowAttachments(
            List<CasePreparationSectionPayloads.AbradingCutRow> rows,
            Long subDeptId) {
        if (rows == null) {
            return;
        }
        for (CasePreparationSectionPayloads.AbradingCutRow row : rows) {
            if (row != null && row.getAttachments() != null) {
                fileMetadataEnrichmentService.enrichDocuments(row.getAttachments(), subDeptId);
            }
        }
    }

    /**
     * Rollback helper to unbind/delete newly promoted files if form save fails.
     */
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

    /**
     * Premix / final-mix ingredient recipe for a Case Prep liner type.
     */
    public CasePrepLinerIngredientsResponse getLinerIngredients(String linerType) {
        CasePrepLinerIngredientsResponse recipe = CasePrepLinerIngredientCatalog.resolve(linerType);
        List<MaterialMaster> activeMaterials = materialRepository.findAll().stream()
                .filter(MaterialMasterDataService::isActive)
                .toList();

        return CasePrepLinerIngredientsResponse.builder()
                .linerType(recipe.getLinerType())
                .fixedPartsByWeight(recipe.isFixedPartsByWeight())
                .premixIngredients(resolveMaterials(recipe.getPremixIngredients(), activeMaterials))
                .finalMixIngredients(resolveMaterials(recipe.getFinalMixIngredients(), activeMaterials))
                .build();
    }

    private static List<CasePrepLinerIngredientDto> resolveMaterials(
            List<CasePrepLinerIngredientDto> ingredients,
            List<MaterialMaster> materials) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        List<CasePrepLinerIngredientDto> resolved = new ArrayList<>(ingredients.size());
        for (CasePrepLinerIngredientDto item : ingredients) {
            MaterialMaster match = findMaterialForIngredient(item, materials);
            resolved.add(CasePrepLinerIngredientDto.builder()
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
            CasePrepLinerIngredientDto item,
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

    /**
     * Normalize display names / codes for case-prep ingredient ↔ materials master matching.
     */
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
