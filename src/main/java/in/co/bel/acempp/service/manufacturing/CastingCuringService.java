package in.co.bel.acempp.service.manufacturing;

import in.co.bel.acempp.dto.common.MotoStageRequest;
import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalRequest;
import in.co.bel.acempp.dto.casepreparation.MotorApprovalResponse;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.castingcuring.*;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.manufacturing.CastingCuring;
import in.co.bel.acempp.entity.manufacturing.CuringCycleMaster;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.manufacturing.CastingCuringRepository;
import in.co.bel.acempp.repository.manufacturing.CuringCycleMasterRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.service.*;
import in.co.bel.acempp.service.batch.MainBatchGateValidator;
import in.co.bel.acempp.service.batch.MainBatchStageConstants;
import in.co.bel.acempp.service.batch.BatchMotorDiscardHelper;
import in.co.bel.acempp.service.batch.MainBatchStageProgressHelper;
import in.co.bel.acempp.service.masterdata.CuringCycleMasterDataService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
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
public class CastingCuringService {

    private final CastingCuringRepository ccRepo;
    private final CuringCycleMasterRepository curingCycleMasterRepo;
    private final BatchRepository batchRepo;
    private final UserRepository userRepo;
    private final BatchService batchService;
    private final MainBatchGateValidator mainBatchGateValidator;
    private final FormIdGeneratorService formIdGenerator;
    private final CastingCuringDtoMapper castingCuringDtoMapper;
    private final FileBindingService fileBindingService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final ValidateMaster validateMaster;

    private final in.co.bel.acempp.service.ApprovalBlockchainAssetService approvalBlockchainAssetService;

    public CuringCyclesResponse getCuringCycles(MotoStageRequest request) {
        String projectId = request != null && request.getProjectId() != null
                ? request.getProjectId().trim()
                : "";
        Integer motorStage = request != null ? request.getMotorStage() : null;
        String curingType = request != null && request.getCuringType() != null
                ? request.getCuringType().trim()
                : "";
        if (projectId.isEmpty() || motorStage == null || curingType.isEmpty()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "projectId, motorStage and curingType are required.");
        }

        CuringCycleMaster configuration = CuringCycleMasterDataService
                .findActiveByProjectIdAndMotorStageAndCuringType(
                        curingCycleMasterRepo, projectId, motorStage, curingType)
                .orElseThrow(() -> {
                    boolean inactiveExists = curingCycleMasterRepo
                            .findAllByProjectIdAndMotorStage(projectId, motorStage)
                            .stream()
                            .anyMatch(e -> Objects.equals(
                                    e.getCuringType() == null ? null : e.getCuringType().trim(),
                                    curingType));
                    String detail = inactiveExists
                            ? " Matching curing cycle master record(s) exist but are inactive;"
                            + " enable one in Curing Cycle master data."
                            : "";
                    return new AcemException(
                            ErrorCode.CONFIGURATION_NOT_FOUND,
                            "No active curing configuration found for project "
                                    + projectId + ", motor stage "
                                    + motorStage + " and curing type "
                                    + curingType + "." + detail);
                });

        List<CuringCyclesResponse.Cycle> cycles =
                CuringCycleMasterDataService.activeSteps(configuration).stream()
                        .map(this::toCuringCycleResponse)
                        .collect(Collectors.toList());

        return CuringCyclesResponse.builder()
                .motorStage(configuration.getMotorStage())
                .motorStageName(configuration.getMotorStageName())
                .curingType(configuration.getCuringType())
                .cycles(cycles)
                .build();
    }

    public CastingCuringFormResponse createForm(CastingCuringRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Batch batch = getBatch(request.getBatchId());
        validateCurrentStage(batch, request.getSubDepartmentId());

        if (ccRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            throw new AcemException(
                    ErrorCode.FORM_ALREADY_EXISTS,
                    "Casting and curing form already submitted for this batch");
        }

        String formId = formIdGenerator.generate("casting_curing", "formId", "CCF", 3);

        // Track successfully promoted files so we can roll them back if anything fails
        List<DocumentDto> promotedDocs = new ArrayList<>();
        try {
            promotedDocs = promoteCastingCuringFiles(
                    request.getMotors(),
                    request.getSubDepartmentId(),
                    formId,
                    Collections.emptySet()
            );
        } catch (Exception e) {
            log.error("File promotion failed during Casting & Curing form creation", e);
            rollbackPromotedFiles(promotedDocs);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload failed, please try again."
            );
        }

        try {

            if (request.getMotors().getFirst().getSetup() != null) {
                validateMaster.validateCastingStation(request.getMotors().getFirst().getSetup());
            }
            if (request.getMotors().getFirst().getCuringSetup() != null && request.getMotors().getFirst().getCuringSetup().getCuringType() != null ){
                validateMaster.validateCuringAndOven(request.getMotors().getFirst().getCuringSetup());
            }
            List<CastingCuringMotor> motors = prepareIncomingMotors(request.getMotors(), batch);
            Status batchStatus = calculateBatchStatus(motors, totalRequiredMotors(batch));
            User creator = getUser(userId);
            LocalDateTime now = LocalDateTime.now();

            CastingCuring entity = CastingCuring.builder()
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

            CastingCuring savedForm = ccRepo.save(entity);
            updateBatchStage(
                    batch,
                    request.getSubDepartmentId(),
                    batchStatus,
                    request.getFormSubmissionType() == Status.SUBMIT || hasSubmittedMotor(motors),
                    userId,
                    creator.getUsername(),
                    now);

            MotorCounts counts = countMotors(motors);
            log.info("Casting & Curing form {} created by {}", formId, userId);

            return CastingCuringFormResponse.builder()
                    .formId(savedForm.getFormId())
                    .batchId(savedForm.getBatchId())
                    .subDepartmentId(savedForm.getSubDepartmentId())
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

    public CastingCuringFormResponse updateForm(CastingCuringRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        CastingCuring form = ccRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Casting and Curing form not found"));

        if (form.getStatus() == Status.APPROVED) {
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
            newlyPromotedDocs = promoteCastingCuringFiles(
                    request.getMotors(),
                    request.getSubDepartmentId(),
                    form.getFormId(),
                    existingFileIds
            );
        } catch (Exception e) {
            log.error("File promotion failed during Casting & Curing form update for formId {}", form.getFormId(), e);
            rollbackPromotedFiles(newlyPromotedDocs);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "File upload failed, please try again."
            );
        }

        try {
            boolean isFinalFormSubmit = request.getFormSubmissionType() == Status.SUBMIT;
            if (request.getMotors().getFirst().getSetup() != null) {
                validateMaster.validateCastingStation(request.getMotors().getFirst().getSetup());
            }
            Map<String, CastingCuringMotor> existingMotorMap = form.getMotors() == null
                    ? new LinkedHashMap<>()
                    : form.getMotors().stream().collect(Collectors.toMap(
                    CastingCuringMotor::getMotorId,
                    Function.identity(),
                    (first, ignored) -> first,
                    LinkedHashMap::new));

            List<CastingCuringMotor> incomingMotors = prepareIncomingMotors(request.getMotors(), batch);
            if (!isFinalFormSubmit) {
                for (CastingCuringMotor incoming : incomingMotors) {
                    CastingCuringMotor existing = existingMotorMap.get(incoming.getMotorId());
                    if (existing != null && isProtectedMotor(existing)) {
                        throw new AcemException(
                                ErrorCode.FORM_ALREADY_EXISTS,
                                "Motor " + incoming.getMotorId() + " cannot be modified.");
                    }
                }
            }

            for (CastingCuringMotor incoming : incomingMotors) {
                CastingCuringMotor existing = existingMotorMap.get(incoming.getMotorId());
                if (existing == null || !isProtectedMotor(existing)) {
                    incoming.setRejectionReason(null);
                    existingMotorMap.put(incoming.getMotorId(), incoming);
                }
            }

            List<CastingCuringMotor> updatedMotors = new ArrayList<>(existingMotorMap.values());
            Status batchStatus = calculateBatchStatus(updatedMotors, totalRequiredMotors(batch));

            User updater = getUser(userId);
            LocalDateTime now = LocalDateTime.now();
            form.setFormSubmissionType(request.getFormSubmissionType());
            form.setStatus(batchStatus);
            form.setSubDepartmentId(request.getSubDepartmentId());
            form.setMotors(updatedMotors);
            form.setRejectionReason(resolveFormRejectionReasonFromMotors(updatedMotors));
            form.setUpdatedBy(PersonInfo.builder().id(userId).fullName(updater.getUsername()).build());
            form.setUpdatedAt(now);
            if (isFinalFormSubmit) {
                form.setSubmittedBy(PersonInfo.builder().id(userId).fullName(updater.getUsername()).build());
                form.setSubmittedAt(now);
            }

            CastingCuring savedForm = ccRepo.save(form);
            updateBatchStage(
                    batch,
                    savedForm.getSubDepartmentId(),
                    batchStatus,
                    isFinalFormSubmit || hasSubmittedMotor(incomingMotors),
                    userId,
                    updater.getUsername(),
                    now);

            MotorCounts counts = countMotors(updatedMotors);
            log.info("Casting & Curing form {} updated by {}", request.getFormId(), userId);

            return CastingCuringFormResponse.builder()
                    .formId(savedForm.getFormId())
                    .batchId(savedForm.getBatchId())
                    .subDepartmentId(savedForm.getSubDepartmentId())
                    .status(batchStatus)
                    .motorCount(updatedMotors.size())
                    .submittedBy(savedForm.getSubmittedBy() != null
                            ? savedForm.getSubmittedBy()
                            : null)
                    .submittedAt(savedForm.getSubmittedAt())
                    .lastUpdatedBy(userId)
                    .lastUpdatedAt(now)
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

    public CastingCuringFormResponse getFormDetails(String formId) {
        CastingCuring form = ccRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "No Casting-Curing form exists for formId " + formId));
        Batch batch = getBatch(form.getBatchId());
        List<CastingCuringMotor> motors = form.getMotors() != null
                ? form.getMotors()
                : Collections.emptyList();
        enrichCastingCuringMotorFiles(motors, form.getSubDepartmentId());
        for (CastingCuringMotor motor : motors) {
            if (motor != null && BatchMotorDiscardHelper.isDiscarded(batch, motor.getMotorId())) {
                motor.setMotorSubmissionStatus(Status.DISCARDED);
            }
        }
        MotorCounts counts = countMotors(motors);
        int totalMotorCount = totalRequiredMotors(batch);
        ProjectInfo project = ProjectInfo.builder()
                .projectId(batch.getProjectId())
                .projectName(batch.getProjectName())
                .build();

        return CastingCuringFormResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .subDepartmentId(form.getSubDepartmentId())
                .formSubmissionType(form.getFormSubmissionType())
                .status(form.getStatus())
                .project(project)
                .motors(motors)
                .allMotorsApproved(counts.approved() == totalMotorCount)
                .pendingMotorCount(counts.pending())
                .approvedMotorCount(counts.approved())
                .rejectedMotorCount(counts.rejected())
                .inProgressMotorCount(counts.inProgress())
                .totalMotorCount(totalMotorCount)
                .motorStatuses(buildFullMotorStatuses(batch, motors, batch.getMotorIds()))
                .discardedMotorIds(BatchMotorDiscardHelper.discardedMotorIds(batch))
                .createdBy(batch.getCreatedBy() != null ? batch.getCreatedBy().getId() : null)
                .createdAt(batch.getCreatedAt())
                .submittedBy(form.getSubmittedBy())
                .submittedAt(form.getSubmittedAt())
                .lastUpdatedBy(form.getUpdatedBy() != null ? form.getUpdatedBy().getId() : null)
                .lastUpdatedAt(form.getUpdatedAt())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public MotorApprovalResponse changeStatusOfMotor(
            MotorApprovalRequest request,
            String approverName) {

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

        CastingCuring form = ccRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Casting and curing form not found"));
        if (request.getSubDepartmentId() != null
                && !request.getSubDepartmentId().equals(form.getSubDepartmentId())) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "The motor approval request does not belong to casting and curing");
        }
        if (form.getMotors() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "No motors are available in this form");
        }
        CastingCuringMotor motor = form.getMotors().stream()
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
        form.setUpdatedBy(PersonInfo.builder().fullName(approverName).build());
        form.setUpdatedAt(now);
        form.setRemarks(request.getRemarks());

        Batch batch = getBatch(form.getBatchId());
        List<CastingCuringMotor> motors = form.getMotors();
        form.setRejectionReason(resolveFormRejectionReasonFromMotors(motors));
        int totalMotorCount = totalRequiredMotors(batch);
        Status batchStatus = calculateBatchStatus(motors, totalMotorCount);
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
                Map<String, Object> blockchainPayload = buildMotorBlockchainPayload(form, motor, approverName, now);
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
        if (batchStatus == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }

        ccRepo.save(form);

        updateBatchAfterMotorApproval(
                batch,
                form,
                batchStatus,
                approverName,
                now);

        MotorCounts counts = countMotors(motors);
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
                .actionBy(approverName)
                .actionAt(now)
                .build();
    }

    private Map<String, Object> buildMotorBlockchainPayload(CastingCuring form, CastingCuringMotor motor, String approverName, LocalDateTime approvalTime) {
        Map<String, Object> rootPayload = new LinkedHashMap<>();

        // 1. Audit / Metadata Object (Created & Approved info separated)
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form != null) {
            if (form.getSubmittedBy() != null) {
                auditMap.put("createdBy", form.getSubmittedBy().getFullName());
            }
            if (form.getSubmittedAt() != null) {
                auditMap.put("createdAt", form.getSubmittedAt());
            }
        }

        // Use passed approver details or fallback to form's updated fields if already set
        if (approverName != null) {
            auditMap.put("approvedBy", approverName);
            auditMap.put("approvedAt", approvalTime);
        } else if (form != null && form.getUpdatedBy() != null) {
            auditMap.put("approvedBy", form.getUpdatedBy().getFullName());
            auditMap.put("approvedAt", form.getUpdatedAt());
        }
        rootPayload.put("audit", auditMap);

        // 2. Motor Technical & Process Details Object
        Map<String, Object> motorMap = new LinkedHashMap<>();
        if (motor != null) {
            motorMap.put("motorId", motor.getMotorId());

            // 1. Setup
            if (motor.getSetup() != null) {
                var setup = motor.getSetup();
                Map<String, Object> setupMap = new LinkedHashMap<>();
                setupMap.put("castingType", setup.getCastingType());
                setupMap.put("castingStation", setup.getCastingStation());
                motorMap.put("setup", setupMap);
            }

            // 2. Curing Setup
            if (motor.getCuringSetup() != null) {
                var curingSetup = motor.getCuringSetup();
                Map<String, Object> curingSetupMap = new LinkedHashMap<>();
                curingSetupMap.put("oven", curingSetup.getOven());
                curingSetupMap.put("ovenNo", curingSetup.getOvenNo());
                curingSetupMap.put("curingType", curingSetup.getCuringType());
                motorMap.put("curingSetup", curingSetupMap);
            }

            // 3. Casting Sections
            if (motor.getCastingSections() != null) {
                var castingSec = motor.getCastingSections();
                Map<String, Object> castingSecMap = new LinkedHashMap<>();

                // Final Assembly Details
                if (castingSec.getFinalAssemblyDetails() != null) {
                    var assembly = castingSec.getFinalAssemblyDetails();
                    Map<String, Object> assemblyMap = new LinkedHashMap<>();
                    assemblyMap.put("motorCasing", assembly.getMotorCasing() == null ? Collections.emptyList() : assembly.getMotorCasing());
                    castingSecMap.put("finalAssemblyDetails", assemblyMap);
                }

                // Casting Process
                if (castingSec.getCastingProcess() != null) {
                    var process = castingSec.getCastingProcess();
                    Map<String, Object> processMap = new LinkedHashMap<>();
                    processMap.put("finalMixBowlDetails", process.getFinalMixBowlDetails() == null ? Collections.emptyList() : process.getFinalMixBowlDetails());
                    processMap.put("castingFromBowlDetails", process.getCastingFromBowlDetails() == null ? Collections.emptyList() : process.getCastingFromBowlDetails());
                    processMap.put("initialVacuum", process.getInitialVacuum());
                    processMap.put("vacuumPressureCasting", process.getVacuumPressureCasting());
                    processMap.put("vacuumPressureSoaking", process.getVacuumPressureSoaking());
                    castingSecMap.put("castingProcess", processMap);
                }

                // Slurry Cast Details
                if (castingSec.getSlurryCastDetails() != null) {
                    var slurry = castingSec.getSlurryCastDetails();
                    Map<String, Object> slurryMap = new LinkedHashMap<>();
                    slurryMap.put("slurryCastFromBowls", slurry.getSlurryCastFromBowls() == null ? Collections.emptyList() : slurry.getSlurryCastFromBowls());
                    castingSecMap.put("slurryCastDetails", slurryMap);
                }

                // Post Cast Operations
                if (castingSec.getPostCastOperations() != null) {
                    var postCast = castingSec.getPostCastOperations();
                    Map<String, Object> postCastMap = new LinkedHashMap<>();
                    postCastMap.put("postCastTable", postCast.getPostCastTable() == null ? Collections.emptyList() : postCast.getPostCastTable());
                    castingSecMap.put("postCastOperations", postCastMap);
                }

                motorMap.put("castingSections", castingSecMap);
            }

            // 4. Curing Sections
            if (motor.getCuringSections() != null) {
                var curingSec = motor.getCuringSections();
                Map<String, Object> curingSecMap = new LinkedHashMap<>();

                // Curing Cycles
                if (curingSec.getCuringCycles() != null) {
                    var cycles = curingSec.getCuringCycles();
                    Map<String, Object> cyclesMap = new LinkedHashMap<>();
                    cyclesMap.put("curingTable", cycles.getCuringTable() == null ? Collections.emptyList() : cycles.getCuringTable());
                    curingSecMap.put("curingCycles", cyclesMap);
                }

                // Post Curing Details
                if (curingSec.getPostCuringDetails() != null) {
                    var postCuring = curingSec.getPostCuringDetails();
                    Map<String, Object> postCuringMap = new LinkedHashMap<>();
                    postCuringMap.put("visualObservation", postCuring.getVisualObservation());
                    postCuringMap.put("shoreAHardness", postCuring.getShoreAHardness());
                    postCuringMap.put("otherObservations", postCuring.getOtherObservations());
                    curingSecMap.put("postCuringDetails", postCuringMap);
                }

                // Decoring Details
                if (curingSec.getDecoringDetails() != null) {
                    var decoring = curingSec.getDecoringDetails();
                    Map<String, Object> decoringMap = new LinkedHashMap<>();
                    decoringMap.put("buildingNo", decoring.getBuildingNo());
                    decoringMap.put("decoringLoad", decoring.getDecoringLoad());
                    decoringMap.put("decoringRemarks", decoring.getDecoringRemarks());
                    decoringMap.put("decoringDateTime", decoring.getDecoringDateTime());
                    List<String> visualObservationHashes = Collections.emptyList();
                    if (decoring.getDecoringVisualObservation() != null && !decoring.getDecoringVisualObservation().isEmpty()) {
                        visualObservationHashes = decoring.getDecoringVisualObservation().stream()
                                .map(DocumentDto::getFileId)
                                .map(this::getFileHash)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    }
                    decoringMap.put("files", visualObservationHashes);
                    curingSecMap.put("decoringDetails", decoringMap);
                }

                motorMap.put("curingSections", curingSecMap);
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
            String approverName) {

        CastingCuring entity = ccRepo.findByFormIdAndIsDeletedFalse(request.getFormId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Casting and curing form not found: " + request.getFormId()));

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

        ccRepo.save(entity);
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

    private List<CastingCuringMotor> prepareIncomingMotors(
            List<CastingCuringMotor> motorForms,
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

        Map<String, CastingCuringMotor> motorsById = new LinkedHashMap<>();
        for (CastingCuringMotor motorForm : motorForms) {
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
            if (submissionType != Status.DRAFT && submissionType != Status.SUBMIT) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorSubmissionType must be DRAFT or SUBMIT for motor " + motorId);
            }
            mainBatchGateValidator.assertCastingCuringMotorUnlocked(batch, motorId);
            if (submissionType == Status.SUBMIT) {
                mainBatchGateValidator.assertCastingCuringMotorCanBeSubmitted(batch, motorId);
            }

            CastingCuringMotor completeMotorForm = castingCuringDtoMapper.toPersistence(motorForm);
            completeMotorForm.setMotorSubmissionStatus(
                    submissionType == Status.DRAFT
                            ? Status.IN_PROGRESS
                            : Status.WAITING_FOR_APPROVAL);
            completeMotorForm.setRejectionReason(null);

            motorsById.put(
                    motorId,
                    completeMotorForm);
        }

        return new ArrayList<>(motorsById.values());
    }

    private static String resolveFormRejectionReasonFromMotors(List<CastingCuringMotor> motors) {
        if (motors == null) {
            return null;
        }
        return motors.stream()
                .filter(m -> m != null && m.getMotorSubmissionStatus() == Status.REJECTED)
                .map(CastingCuringMotor::getRejectionReason)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<MotorStatusResponse> buildFullMotorStatuses(
            Batch batch,
            List<CastingCuringMotor> savedMotors,
            List<String> batchMotorIds) {

        Map<String, CastingCuringMotor> savedMap = savedMotors == null
                ? Collections.emptyMap()
                : savedMotors.stream().collect(Collectors.toMap(
                CastingCuringMotor::getMotorId,
                Function.identity(),
                (first, ignored) -> first));

        List<MotorStatusResponse> statuses = new ArrayList<>();
        for (String motorId : batchMotorIds) {
            CastingCuringMotor motor = savedMap.get(motorId);
            statuses.add(motor == null
                    ? MotorStatusResponse.builder()
                    .motorId(motorId)
                    .motorSubmissionType(Status.DRAFT)
                    .motorSubmissionStatus(MainBatchStageProgressHelper.defaultMotorSubmissionStatus(
                            batch, MainBatchStageConstants.CASTING_CURING, motorId))
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
            List<CastingCuringMotor> savedMotors,
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
        if (approvedCount > 0) {
            return Status.IN_PROGRESS;
        }
        boolean anyStarted = savedMotors.stream()
                .anyMatch(m -> motorStatus(m) == Status.IN_PROGRESS
                        || motorSubmissionType(m) == Status.DRAFT);
        return anyStarted ? Status.IN_PROGRESS : Status.TO_BE_INITIATED;
    }

    private MotorCounts countMotors(List<CastingCuringMotor> motors) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int inProgress = 0;

        for (CastingCuringMotor motor : motors) {
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

    private boolean isProtectedMotor(CastingCuringMotor motor) {
        Status status = motorStatus(motor);
        return status == Status.APPROVED || status == Status.WAITING_FOR_APPROVAL;
    }

    private Status motorSubmissionType(CastingCuringMotor motor) {
        return motor.getMotorSubmissionType();
    }

    private boolean hasSubmittedMotor(List<CastingCuringMotor> motors) {
        return motors != null && motors.stream()
                .filter(motor -> motor != null)
                .anyMatch(motor -> motorSubmissionType(motor) == Status.SUBMIT);
    }

    private Status motorStatus(CastingCuringMotor motor) {
        return motor.getMotorSubmissionStatus();
    }

    private CuringCyclesResponse.Cycle toCuringCycleResponse(
            CuringCycleMaster.Cycle cycle) {
        return CuringCyclesResponse.Cycle.builder()
                .sequenceNo(cycle.getSequenceNo() != null ? cycle.getSequenceNo() : 0)
                .temperature(cycle.getTemperature() != null ? cycle.getTemperature() : 0)
                .durationMinutes(cycle.getDurationMinutes() != null ? cycle.getDurationMinutes() : 0)
                .startDateTime(cycle.getStartDateTime())
                .endDateTime(cycle.getEndDateTime())
                .hotWaterCirculation(cycle.getHotWaterCirculation())
                .build();
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
        batchService.assertStageUnlockedAndActive(batch, subDepartmentId, "Casting and Curing");
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

        batchService.syncStageProgress(
                batch.getBatchId(),
                subDepartmentId,
                status,
                null,
                null,
                buildFullMotorStatuses(
                        batch,
                        ccRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                                .map(CastingCuring::getMotors)
                                .orElse(Collections.emptyList()),
                        batch.getMotorIds()),
                submitted ? PersonInfo.builder().id(userId).fullName(username).build() : null,
                now,
                false,
                null,
                null);
    }

    private void updateBatchAfterMotorApproval(
            Batch batch,
            CastingCuring form,
            Status batchStatus,
            String approverUserId,
            LocalDateTime now) {

        List<MotorStatusResponse> motorStatuses =
                buildFullMotorStatuses(batch, form.getMotors(), batch.getMotorIds());
        boolean anyApproved = motorStatuses.stream()
                .anyMatch(status -> status.getMotorSubmissionStatus() == Status.APPROVED);
        batchService.syncStageApproval(
                batch.getBatchId(),
                form.getSubDepartmentId(),
                batchStatus,
                null,
                null,
                motorStatuses,
                anyApproved,
                form.getRemarks(),
                form.getRejectionReason(),
                approverUserId,
                now);
    }

    private record MotorCounts(
            int pending,
            int approved,
            int rejected,
            int inProgress) {
    }

    private Set<String> extractExistingFormFileIds(CastingCuring form) {
        Set<String> fileIds = new HashSet<>();
        if (form == null || form.getMotors() == null) {
            return fileIds;
        }

        for (CastingCuringMotor motor : form.getMotors()) {
            if (motor != null && motor.getCuringSections() != null
                    && motor.getCuringSections().getDecoringDetails() != null) {

                extractFileIdsFromDocs(
                        motor.getCuringSections().getDecoringDetails().getDecoringVisualObservation(),
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

    private List<DocumentDto> promoteCastingCuringFiles(
            List<CastingCuringMotor> motors,
            Long subDeptId,
            String formId,
            Set<String> existingFileIds) {

        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        if (motors == null || motors.isEmpty()) {
            return newlyPromotedDocs;
        }

        for (CastingCuringMotor motor : motors) {
            if (motor == null) {
                continue;
            }

            if (motor.getCuringSections() != null
                    && motor.getCuringSections().getDecoringDetails() != null) {

                List<DocumentDto> docs = motor.getCuringSections().getDecoringDetails().getDecoringVisualObservation();
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

    private Set<String> extractMotorFileIds(CastingCuringMotor motor) {
        Set<String> fileIds = new HashSet<>();
        if (motor != null && motor.getCuringSections() != null
                && motor.getCuringSections().getDecoringDetails() != null) {
            fileIds.addAll(FileIdExtractionUtil.extractFromDocuments(
                    motor.getCuringSections().getDecoringDetails().getDecoringVisualObservation()));
        }
        return fileIds;
    }

    private void enrichCastingCuringMotorFiles(List<CastingCuringMotor> motors, Long subDeptId) {
        if (motors == null) {
            return;
        }
        for (CastingCuringMotor motor : motors) {
            if (motor != null && motor.getCuringSections() != null
                    && motor.getCuringSections().getDecoringDetails() != null) {
                fileMetadataEnrichmentService.enrichDocuments(
                        motor.getCuringSections().getDecoringDetails().getDecoringVisualObservation(),
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
}
