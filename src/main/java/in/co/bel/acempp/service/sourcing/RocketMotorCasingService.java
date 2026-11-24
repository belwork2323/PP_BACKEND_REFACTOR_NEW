package in.co.bel.acempp.service.sourcing;

import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.general.ProjectDto;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.ndt.motor.RadiographyPlanDetailsDto;
import in.co.bel.acempp.dto.rocketmotorcasing.*;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.*;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.Project;
import in.co.bel.acempp.entity.InsulationSpecification;
import in.co.bel.acempp.entity.sourcing.RocketMotorCasing;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.InsulationSpecificationRepository;
import in.co.bel.acempp.repository.ProjectRepository;
import in.co.bel.acempp.repository.sourcing.RocketMotorCasingRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.service.FileBindingService;
import in.co.bel.acempp.service.FileMetadataEnrichmentService;
import in.co.bel.acempp.service.FileReadOnlyService;
import in.co.bel.acempp.service.FormIdGeneratorService;
import in.co.bel.acempp.service.batch.MotorBatchLifecycleHelper;
import in.co.bel.acempp.service.masterdata.InsulationSpecificationMasterDataService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RocketMotorCasingService {

    private static final Pattern MOTOR_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 /_-]*$");
    /** Matches FE VALIDATIONSTRING.LENGTH.MAX_STANDARD */
    private static final int MAX_STANDARD_FIELD_LENGTH = 100;
    private static final Pattern ALPHA_NUM_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 /_-]*$");

    private final RocketMotorCasingRepository repository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final FormIdGeneratorService formIdGenerator;
    private final FileBindingService fileBindingService;
    private final RmcSectionsMapper rmcSectionsMapper;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final InsulationSpecificationRepository insulationSpecificationRepository;
    private final MongoTemplate mongoTemplate;

    @Transactional(rollbackFor = Exception.class)
    public RmcFormResponse createForm(CreateRmcFormRequest request, String userId) {
        log.info("Creating Rocket Motor Casing form for userId={}", userId);

        validateRmcForTier(request);

        boolean isDraft = request.getFormSubmissionType() == Status.DRAFT;
        String motorCasingId = generateMotorCasingId(request.getMotorStage());
        Status status;

        if (isDraft) {
            status = Status.IN_PROGRESS;
        } else {
            if (repository.existsByMotorCasingId(motorCasingId)) {
                throw new AcemException(
                        ErrorCode.DUPLICATE_CASING,
                        "Casing already exists",
                        null
                );
            }
            status = Status.WAITING_FOR_APPROVAL;
        }

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findByProjectId(request.getProjectId())
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.DEPENDENCY_NOT_FOUND,
                            "Project not found: " + request.getProjectId()));
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        String username = user.getUsername();

        List<String> promotedFileIds = new ArrayList<>();

        try {
            Sections persistedSections = rmcSectionsMapper.toPersistence(request.getSections());
            if (persistedSections != null) {
                // Pass Collections.emptySet() since no existing files exist in DB during creation
                promotedFileIds = promoteSectionFiles(
                        persistedSections,
                        request.getSubDepartmentId(),
                        motorCasingId,
                        Collections.emptySet()
                );
            }

            RocketMotorCasing entity = RocketMotorCasing.builder()
                    .subDepartmentId(request.getSubDepartmentId())
                    .projectId(request.getProjectId())
                    .projectName(project != null ? project.getProjectName() : null)
                    .motorStage(request.getMotorStage())
                    .motorId(request.getMotorId())
                    .motorCasingId(motorCasingId)
                    .formSubmissionType(request.getFormSubmissionType())
                    .status(status)
                    .nextStep(isDraft ? "DRAFT" : "MOCK_TRIAL")
                    .sections(persistedSections)
                    .active(true)
                    .createdByUserId(userId)
                    .createdByUsername(username)
                    .updatedBy(PersonInfo.builder()
                            .id(userId)
                            .fullName(username)
                            .build())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            repository.save(entity);

            return RmcFormResponse.builder()
                    .motorCasingId(entity.getMotorCasingId())
                    .status(entity.getStatus())
                    .build();

        } catch (Exception ex) {
            log.error("Rocket Motor Casing creation failed for motorCasingId={}. Initiating file promotion rollback...", motorCasingId, ex);

            rollbackPromotedFiles(promotedFileIds);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to create Rocket Motor Casing form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public RmcFormResponse updateForm(UpdateRmcFormRequest request, String userId) {
        log.info("Updating Rocket Motor Casing form for motorCasingId={}, userId={}", request.getMotorCasingId(), userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        String username = user.getUsername();

        RocketMotorCasing entity = repository.findByMotorCasingId(request.getMotorCasingId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Form not found",
                        "motorCasingId: " + request.getMotorCasingId()
                ));

        if (entity.getStatus() == Status.WAITING_FOR_APPROVAL || entity.getStatus() == Status.APPROVED) {
            throw new AcemException(
                    ErrorCode.FORM_LOCKED,
                    "Form cannot be updated",
                    "Current status: " + entity.getStatus()
            );
        }

        validateUpdateRmcForTier(request, entity);

        List<String> promotedFileIds = new ArrayList<>();

        try {
            // PATCH UPDATE - BASIC FIELDS
            if (request.getSubDepartmentId() != null) {
                entity.setSubDepartmentId(request.getSubDepartmentId());
            }
            if (request.getProjectId() != null && !request.getProjectId().isBlank()) {
                entity.setProjectId(request.getProjectId());
            }
            if (request.getMotorStage() != null) {
                entity.setMotorStage(request.getMotorStage());
            }
            if (request.getMotorId() != null && !request.getMotorId().isBlank()) {
                entity.setMotorId(request.getMotorId());
            }

            // PUT UPDATE - SECTIONS
            if (request.getSections() != null) {
                Sections incoming = rmcSectionsMapper.toPersistence(request.getSections());
                Sections existing = entity.getSections();

                if (existing == null) {
                    existing = new Sections();
                }

                // 1. Collect file IDs from Database and Request Payload
                Set<String> existingDbFileIds = extractAllFileIds(existing);
                Set<String> incomingFileIds = extractAllFileIds(incoming);

                // 2. Identify file IDs removed by user (in DB, absent in Payload)
                List<String> removedFileIds = existingDbFileIds.stream()
                        .filter(fileId -> !incomingFileIds.contains(fileId))
                        .collect(Collectors.toList());

                // 3. Promote only NEW files
                promotedFileIds = promoteSectionFiles(
                        incoming, request.getSubDepartmentId(), entity.getMotorCasingId(), existingDbFileIds);

                // 4. Purge removed files permanently from Disk and Mongo DB
                if (!removedFileIds.isEmpty()) {
                    fileBindingService.purgeRemovedFilesFromDiskAndDb(entity.getMotorCasingId(), removedFileIds);
                }

                // MOTOR RECEIPT
                if (incoming.getMotorReceipt() != null) {
                    MotorReceipt existingReceipt = existing.getMotorReceipt();
                    if (existingReceipt == null) {
                        existingReceipt = new MotorReceipt();
                    }

                    MotorReceipt incomingReceipt = incoming.getMotorReceipt();

                    if (incomingReceipt.getCasingType() != null) {
                        existingReceipt.setCasingType(incomingReceipt.getCasingType());
                    }
                    if (incomingReceipt.getReceivingDate() != null) {
                        existingReceipt.setReceivingDate(incomingReceipt.getReceivingDate());
                    }
                    if (incomingReceipt.getItemsReceived() != null) {
                        existingReceipt.setItemsReceived(incomingReceipt.getItemsReceived());
                    }
                    if (incomingReceipt.getClearances() != null) {
                        existingReceipt.setClearances(incomingReceipt.getClearances());
                    }
                    if (incomingReceipt.getNdtUtReport() != null) {
                        existingReceipt.setNdtUtReport(incomingReceipt.getNdtUtReport());
                    }
                    if (incomingReceipt.getAcemNdtObservations() != null) {
                        existingReceipt.setAcemNdtObservations(incomingReceipt.getAcemNdtObservations());
                    }
                    if (incomingReceipt.getProjectRubberSurfaceObservations() != null) {
                        existingReceipt.setProjectRubberSurfaceObservations(incomingReceipt.getProjectRubberSurfaceObservations());
                    }
                    if (incomingReceipt.getOtherDetails() != null) {
                        existingReceipt.setOtherDetails(incomingReceipt.getOtherDetails());
                    }

                    if (incomingReceipt.getInsulation() != null) {
                        Insulation existingInsulation = existingReceipt.getInsulation();
                        if (existingInsulation == null) {
                            existingInsulation = new Insulation();
                        }

                        Insulation incomingInsulation = incomingReceipt.getInsulation();

                        if (incomingInsulation.getInsulationCuringDate() != null) {
                            existingInsulation.setInsulationCuringDate(incomingInsulation.getInsulationCuringDate());
                        }
                        if (incomingInsulation.getType() != null) {
                            existingInsulation.setType(incomingInsulation.getType());
                        }
                        if (incomingInsulation.getReportNo() != null) {
                            existingInsulation.setReportNo(incomingInsulation.getReportNo());
                        }
                        if (incomingInsulation.getReceiptStatus() != null) {
                            existingInsulation.setReceiptStatus(incomingInsulation.getReceiptStatus());
                        }
                        if (incomingInsulation.getReportUpload() != null) {
                            existingInsulation.setReportUpload(incomingInsulation.getReportUpload());
                        }
                        if (incomingInsulation.getInsulationSpecification() != null) {
                            existingInsulation.setInsulationSpecification(incomingInsulation.getInsulationSpecification());
                        }

                        existingReceipt.setInsulation(existingInsulation);
                    }

                    existing.setMotorReceipt(existingReceipt);
                }

                // VISUAL INSPECTION
                if (incoming.getVisualInspection() != null) {
                    existing.setVisualInspection(incoming.getVisualInspection());
                }

                // WEIGHTMENT
                if (incoming.getWeightment() != null) {
                    existing.setWeightment(incoming.getWeightment());
                }

                // DIMENSIONAL INSPECTION
                if (incoming.getDimensionalInspection() != null) {
                    existing.setDimensionalInspection(incoming.getDimensionalInspection());
                }

                // MOCK TRIAL DETAILS
                if (incoming.getMockTrial() != null) {
                    existing.setMockTrial(incoming.getMockTrial());
                }

                if (incoming.getReportUpload() != null) {
                    ReportUploadDto existingUpload = existing.getReportUpload();
                    if (existingUpload == null) {
                        existingUpload = new ReportUploadDto();
                    }

                    ReportUploadDto incomingUpload = incoming.getReportUpload();

                    if (incomingUpload.getNdtUtReport() != null) {
                        existingUpload.setNdtUtReport(incomingUpload.getNdtUtReport());
                    }
                    if (incomingUpload.getVisualInspectionReport() != null) {
                        existingUpload.setVisualInspectionReport(incomingUpload.getVisualInspectionReport());
                    }
                    if (incomingUpload.getWeighmentReport() != null) {
                        existingUpload.setWeighmentReport(incomingUpload.getWeighmentReport());
                    }
                    if (incomingUpload.getDimensionalInspectionReport() != null) {
                        existingUpload.setDimensionalInspectionReport(incomingUpload.getDimensionalInspectionReport());
                    }
                    if (incomingUpload.getMockTrialReport() != null) {
                        existingUpload.setMockTrialReport(incomingUpload.getMockTrialReport());
                    }
                    if (incomingUpload.getInsulationLiningReport() != null) {
                        existingUpload.setInsulationLiningReport(incomingUpload.getInsulationLiningReport());
                    }

                    existing.setReportUpload(existingUpload);
                }

                // RADIOGRAPHY DETAILS
                if (incoming.getRadiographyDetails() != null) {
                    existing.setRadiographyDetails(incoming.getRadiographyDetails());
                }

                entity.setSections(existing);
            }

            // FORM SUBMISSION TYPE
            Status submissionType = request.getFormSubmissionType();
            entity.setFormSubmissionType(submissionType);

            // SUBMIT FLOW
            if (submissionType == Status.SUBMIT) {
                entity.setStatus(Status.WAITING_FOR_APPROVAL);
            } else { // DRAFT FLOW
                entity.setStatus(Status.IN_PROGRESS);
                entity.setNextStep("DRAFT");
            }

            entity.setUpdatedBy(PersonInfo.builder()
                    .id(userId)
                    .fullName(username)
                    .build());
            entity.setUpdatedAt(LocalDateTime.now());

            repository.save(entity);

            return RmcFormResponse.builder()
                    .motorCasingId(entity.getMotorCasingId())
                    .status(entity.getStatus())
                    .build();

        } catch (Exception ex) {
            log.error("Rocket Motor Casing update failed for motorCasingId={}. Initiating file promotion rollback...", request.getMotorCasingId(), ex);

            rollbackPromotedFiles(promotedFileIds);

            if (ex instanceof AcemException acemEx) {
                throw acemEx;
            }
            throw new AcemException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to update Rocket Motor Casing form. Uploaded files have been rolled back: " + ex.getMessage()
            );
        }
    }

    public AvailableMotorListResponse getAvailableMotors(AvailableMotorRequest request) {
        boolean projectExists = repository.existsByProjectIdAndActiveTrue(request.getProjectId());
        if (!projectExists) {
            throw new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "Project not found",
                    "No motor casing records found for projectId: "
                            + request.getProjectId()
            );
        }

        List<RocketMotorCasing> casings = repository.findByProjectIdAndMotorStageAndStatusAndActiveTrue(
                request.getProjectId(),
                request.getMotorStage(),
                Status.APPROVED
        );

        Set<String> candidateIds = casings.stream()
                .map(RocketMotorCasing::getMotorId)
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<String> unavailable = findUnavailableMotorIds(candidateIds);

        List<AvailableMotorResponse> motors = casings.stream()
                .filter(casing -> !MotorBatchLifecycleHelper.containsIgnoreCase(
                        unavailable, casing.getMotorId()))
                .map(casing ->
                        AvailableMotorResponse.builder()
                                .motorCasingId(casing.getMotorCasingId())
                                .motorId(casing.getMotorId())
                                .status(casing.getStatus().name())
                                .build()).toList();

        return AvailableMotorListResponse.builder().
                projectId(request.getProjectId())
                .motorStage(request.getMotorStage())
                .motors(motors)
                .build();
    }

    private Set<String> findUnavailableMotorIds(Set<String> motorIds) {
        if (motorIds == null || motorIds.isEmpty()) {
            return Set.of();
        }
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                Criteria.where("motorIds").in(motorIds)));
        List<Batch> batches = mongoTemplate.find(query, Batch.class);
        return MotorBatchLifecycleHelper.collectUnavailableMotorIds(batches);
    }

    public RmcFormResponse deleteForm(RmcDetailsRequest request, String userId) {

        RocketMotorCasing entity = repository.findByMotorCasingId(request.getMotorCasingId())
                .orElseThrow(() ->
                        new AcemException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "Data not found",
                                "No form found for motorCasingId: "
                                        + request.getMotorCasingId()
                        ));
        if (entity.getFormSubmissionType() == Status.SUBMIT) {
            throw new AcemException(ErrorCode.INVALID_STATE,
                    "Submitted forms cannot be deleted",
                    "Only draft forms can be deleted."
            );
        }

        // ALLOWED STATES
        if (entity.getStatus() != Status.IN_PROGRESS && entity.getStatus() != Status.REJECTED) {
            throw new AcemException(ErrorCode.INVALID_STATE, "Invalid state for deletion",
                    "Only IN_PROGRESS or REJECTED forms can be deleted. Current status: "
                            + entity.getStatus()
            );
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        String username = user.getUsername();
        // SOFT DELETE
        entity.setStatus(Status.DELETED);
        entity.setActive(false);
        entity.setUpdatedBy(PersonInfo.builder().id(userId).fullName(username).build()
        );
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
        return RmcFormResponse.builder().motorCasingId(entity.getMotorCasingId()).status(entity.getStatus()).build();
    }

    private String generateMotorCasingId(Integer motorStage) {
        return formIdGenerator.generate(
                "rocket_motor_casing",
                "motorCasingId",
                "RMC-" + motorStage,
                3
        );
    }

    public RmcDetailsResponse getFormDetails(RmcDetailsRequest request) {

        RocketMotorCasing form = repository.findByMotorCasingId(request.getMotorCasingId())
                .orElseThrow(() ->
                        new AcemException(ErrorCode.FORM_NOT_FOUND, "Rocket Motor Casing form not found",
                                "No form found for motorCasingId '" + request.getMotorCasingId() + "'."));
        Sections sections = form.getSections();
        if (sections != null) {
            enrichSectionFiles(sections, form.getSubDepartmentId());
            enrichInsulationSpecificationMetadata(sections, isMasterRowFrozen(form.getStatus()));
        }
        MotorReceipt receipt = sections != null ? sections.getMotorReceipt() : null;
        Insulation insulation = receipt != null ? receipt.getInsulation() : null;
        String insulationType = insulation != null && insulation.getType() != null && !insulation.getType().isBlank()
                ? insulation.getType()
                : null;
        return RmcDetailsResponse.builder()
                .motorCasingId(form.getMotorCasingId())
                .project(
                        ProjectDto.builder()
                                .projectId(form.getProjectId())
                                .projectName(form.getProjectName())
                                .build()
                )
                .subDepartmentId(form.getSubDepartmentId())
                .motorStage(form.getMotorStage())
                .motorId(form.getMotorId())
                .status(form.getStatus())
                .casingType(receipt != null ? receipt.getCasingType() : null)
                .insulationType(insulationType)
                .receivingDate(receipt != null ? receipt.getReceivingDate() : null)
                .createdAt(form.getCreatedAt())
                .createdOn(form.getCreatedAt())
                .createdBy(form.getCreatedByUserId())
                .createdByInfo(PersonInfo.builder()
                        .id(form.getCreatedByUserId())
                        .fullName(form.getCreatedByUsername())
                        .build())
                .updatedAt(form.getUpdatedAt())
                .sections(rmcSectionsMapper.toResponse(sections))
                .build();
    }

    public MotorsStatsResponse getMotorStats(Long subDepartmentId) {
        long createdMotors = repository.countBySubDepartmentIdAndActiveTrue(subDepartmentId);
        long pendingMotors = repository.countBySubDepartmentIdAndStatusIn(
                subDepartmentId, List.of(Status.TO_BE_INITIATED, Status.IN_PROGRESS));
        long waitingForApprovalMotors = repository.countBySubDepartmentIdAndStatus(
                subDepartmentId, Status.WAITING_FOR_APPROVAL);
        long approvedMotors = repository.countBySubDepartmentIdAndStatus(subDepartmentId, Status.APPROVED);
        long rejectedMotors = repository.countBySubDepartmentIdAndStatus(subDepartmentId, Status.REJECTED);
        return MotorsStatsResponse.builder()
                .createdMotors(createdMotors)
                .pendingMotors(pendingMotors)
                .waitingForApprovalMotors(waitingForApprovalMotors)
                .approvedMotors(approvedMotors)
                .rejectedMotors(rejectedMotors)
                .build();
    }


    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {

        RocketMotorCasing entity = repository.findByMotorCasingIdAndSubDepartmentId(request.getFormId(),
                        request.getSubDepartmentId()
                )
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Not found",
                        "Motor casing not found: "
                                + request.getFormId()
                ));

        ApprovalStatusUtil.applyStatusChange(request.getActionType(), entity.getSubDepartmentId(), request.getRemarks(),
                request.getRejectionReason(),
                approverName, entity::setStatus,
                entity::setRemarks,
                entity::setRejectionReason,
                entity::setUpdatedBy,
                entity::setUpdatedAt
        );
        if (request.getActionType() == Status.APPROVED
                || request.getActionType() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnly(
                    entity.getSubDepartmentId(),
                    extractAllFileIds(entity.getSections()));
        }
        repository.save(entity);
        return ApproverStatusResponse.builder()
                .formId(entity.getMotorCasingId())
                .status(entity.getStatus())
                .remarks(entity.getRemarks())
                .rejectionReason(entity.getRejectionReason())
                .actionAt(entity.getUpdatedAt())
                .actionBy(entity.getUpdatedBy())
                .build();
    }

    // MASTER VALIDATION ENTRY
    private boolean isSubmitIntent(Status submissionType) {
        return submissionType == Status.SUBMIT;
    }

    private void validateRmcForTier(CreateRmcFormRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateIdentification(request.getProjectId(), request.getMotorStage(), request.getMotorId(), errors);

        if (isSubmitIntent(request.getFormSubmissionType())) {
        Sections sections = rmcSectionsMapper.toPersistence(request.getSections());
            validateSectionsForSubmit(sections, errors);
        }

        throwValidationErrors(errors);
    }

    private void validateUpdateRmcForTier(UpdateRmcFormRequest request, RocketMotorCasing entity) {
        Map<String, String> errors = new LinkedHashMap<>();

        String projectId = !isBlank(request.getProjectId()) ? request.getProjectId() : entity.getProjectId();
        Integer motorStage = request.getMotorStage() != null ? request.getMotorStage() : entity.getMotorStage();
        String motorId = !isBlank(request.getMotorId()) ? request.getMotorId() : entity.getMotorId();
        validateIdentification(projectId, motorStage, motorId, errors);

        if (isSubmitIntent(request.getFormSubmissionType())) {
            Sections sections = request.getSections() != null
                    ? rmcSectionsMapper.toPersistence(request.getSections())
                    : entity.getSections();
            validateSectionsForSubmit(sections, errors);
        }

        throwValidationErrors(errors);
    }

    private void validateIdentification(
            String projectId,
            Integer motorStage,
            String motorId,
            Map<String, String> errors) {
        if (isBlank(projectId)) {
            errors.put("projectId", "projectId is required");
        }
        if (motorStage == null || motorStage < 0) {
            errors.put("motorStage", "motorStage is required");
        }
        if (isBlank(motorId)) {
            errors.put("motorId", "motorId is required");
        } else if (!MOTOR_ID_PATTERN.matcher(motorId.trim()).matches()) {
            errors.put("motorId", "motorId is invalid");
        } else if (motorId.trim().length() > MAX_STANDARD_FIELD_LENGTH) {
            errors.put("motorId", "motorId cannot exceed " + MAX_STANDARD_FIELD_LENGTH + " characters");
        }
    }

    private void validateSectionsForSubmit(Sections sections, Map<String, String> errors) {
        if (sections == null) {
            errors.put("sections", "sections are required");
            return;
        }

        validateMotorReceiptForSubmit(sections.getMotorReceipt(), errors);
        validateVisualInspectionForSubmit(sections.getVisualInspection(), errors);
        validateWeightmentForSubmit(sections.getWeightment(), errors);
        validateDimensionalInspectionForSubmit(sections.getDimensionalInspection(), errors);
        validateMockTrialForSubmit(sections.getMockTrial(), errors);
        validateReportUploadForSubmit(sections.getReportUpload(), errors);
        validateRadiographyForSubmit(sections.getRadiographyDetails(), errors);
    }

    private void validateMotorReceiptForSubmit(MotorReceipt receipt, Map<String, String> errors) {
        if (receipt == null) {
            errors.put("motorReceipt", "motor receipt is required");
            return;
        }

        if (isBlank(receipt.getCasingType())) {
            errors.put("casingType", "casingType is required");
        }
        if (receipt.getReceivingDate() == null) {
            errors.put("receivingDate", "receivingDate is required");
        }

        Insulation insulation = receipt.getInsulation();
        if (insulation == null) {
            errors.put("insulation", "insulation is required");
            return;
        }

        if (insulation.getInsulationCuringDate() == null) {
            errors.put("insulationCuringDate", "insulationCuringDate is required");
        }
        if (isBlank(insulation.getType())) {
            errors.put("insulationType", "insulationType is required");
        }
        if (isBlank(insulation.getReportNo())) {
            errors.put("insulationReportNo", "insulationReportNo is required");
        } else if (!ALPHA_NUM_PATTERN.matcher(insulation.getReportNo().trim()).matches()) {
            errors.put("insulationReportNo", "insulationReportNo is invalid");
        }

        validateInsulationSpecificationForSubmit(insulation, errors);
    }

    private void validateInsulationSpecificationForSubmit(Insulation insulation, Map<String, String> errors) {
        if (isBlank(insulation.getType())) {
            return;
        }

        InsulationSpecificationDto specification = insulation.getInsulationSpecification();
        if (specification == null || specification.getSpecifications() == null
                || specification.getSpecifications().isEmpty()) {
            errors.put("insulationSpecification", "insulationSpecification is required");
            return;
        }

        InsulationSpecification master = insulationSpecificationRepository
                .findByInsulationTypeIgnoreCase(insulation.getType())
                .orElse(null);

        if (master != null && !InsulationSpecificationMasterDataService.isActive(master)) {
            errors.put("insulationType", "Insulation specification master is inactive for type "
                    + insulation.getType());
            return;
        }

        Map<String, ParameterDto> masterByCode = new LinkedHashMap<>();
        if (master != null && master.getSpecifications() != null) {
            InsulationSpecificationMasterDataService.filterActiveSpecifications(master.getSpecifications())
                    .stream()
                    .map(CategoryDto::getParameters)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .filter(param -> !isBlank(param.getSpecificationCode()))
                    .forEach(param -> masterByCode.put(param.getSpecificationCode().trim(), param));
        }

        Set<String> requiredCodes = masterByCode.keySet();

        Map<String, SpecificationParameterDto> submittedByCode = specification.getSpecifications().stream()
                .filter(Objects::nonNull)
                .map(SpecificationCategoryDto::getParameters)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .filter(param -> !isBlank(param.getSpecificationCode()))
                .collect(Collectors.toMap(
                        param -> param.getSpecificationCode().trim(),
                        param -> param,
                        (left, right) -> left,
                        LinkedHashMap::new));

        for (String code : requiredCodes) {
            if (!submittedByCode.containsKey(code)) {
                boolean thermal = isThermalMasterParam(master, code);
                String prefix = thermal ? "thermalProperties." : "mechanicalProperties.";
                errors.put(prefix + code + ".reported", "reported is required");
                errors.put(prefix + code + ".acemSpec", "acemSpec is required");
            }
        }

        for (SpecificationCategoryDto category : specification.getSpecifications()) {
            if (category == null || category.getParameters() == null) {
                continue;
            }
            String categoryName = String.valueOf(category.getCategory()).toLowerCase(Locale.ROOT);
            boolean mechanical = categoryName.contains("mechanical");
            boolean thermal = categoryName.contains("thermal");
            if (!mechanical && !thermal) {
                continue;
            }

            for (SpecificationParameterDto param : category.getParameters()) {
                if (param == null || isBlank(param.getSpecificationCode())) {
                    continue;
                }
                String code = param.getSpecificationCode().trim();
                String prefix = thermal ? "thermalProperties." : "mechanicalProperties.";
                ParameterDto masterParam = masterByCode.get(code);
                in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange range =
                        masterParam != null ? masterParam.getReferenceRange() : null;

                validateInsulationSpecValue(
                        param.getReported(),
                        range,
                        prefix + code + ".reported",
                        "reported",
                        true,
                        errors);
                validateInsulationSpecValue(
                        param.getAcemSpec(),
                        range,
                        prefix + code + ".acemSpec",
                        "acemSpec",
                        true,
                        errors);
            }
        }
    }

    private boolean isThermalMasterParam(InsulationSpecification master, String code) {
        if (master == null || master.getSpecifications() == null || isBlank(code)) {
            return false;
        }
        for (CategoryDto category : InsulationSpecificationMasterDataService.filterActiveSpecifications(
                master.getSpecifications())) {
            if (category == null || category.getParameters() == null) {
                continue;
            }
            String categoryName = String.valueOf(category.getCategory()).toLowerCase(Locale.ROOT);
            if (!categoryName.contains("thermal")) {
                continue;
            }
            for (ParameterDto param : category.getParameters()) {
                if (param != null && code.equals(param.getSpecificationCode() != null
                        ? param.getSpecificationCode().trim() : null)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateInsulationSpecValue(
            String value,
            in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange referenceRange,
            String fieldPath,
            String fieldLabel,
            boolean required,
            Map<String, String> errors) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isEmpty()) {
            if (required) {
                errors.put(fieldPath, fieldLabel + " is required");
            }
            return;
        }

        Double minValue = referenceRange != null ? referenceRange.getMinValue() : null;
        Double maxValue = referenceRange != null ? referenceRange.getMaxValue() : null;

        if (isReferenceRangeNotApplicable(minValue, maxValue)) {
            if (!ALPHA_NUM_PATTERN.matcher(normalized).matches()) {
                errors.put(fieldPath, fieldLabel + " is invalid");
            }
            return;
        }

        if (!isParseableNumeric(normalized)) {
            errors.put(fieldPath, fieldLabel + " must be numeric");
            return;
        }
        if (isOutOfRange(normalized, minValue, maxValue)) {
            errors.put(fieldPath, fieldLabel + " is out of range");
        }
    }

    private boolean isReferenceRangeNotApplicable(Double minValue, Double maxValue) {
        return minValue == null && maxValue == null;
    }

    private boolean isParseableNumeric(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            Double.parseDouble(value.replace(",", "").trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean isOutOfRange(String value, Double minValue, Double maxValue) {
        if (isReferenceRangeNotApplicable(minValue, maxValue)) {
            return false;
        }
        if (isBlank(value)) {
            return true;
        }
        try {
            double parsed = Double.parseDouble(value.replace(",", "").trim());
            if (minValue != null && parsed < minValue) {
                return true;
            }
            return maxValue != null && parsed > maxValue;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private void validateVisualInspectionForSubmit(List<VisualInspection> rows, Map<String, String> errors) {
        if (rows == null || rows.isEmpty()) {
            errors.put("visualInspection", "visualInspection is required");
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            VisualInspection row = rows.get(i);
            if (row == null) {
                errors.put("visualInspection[" + i + "]", "visualInspection row is required");
                continue;
            }
            String obsPath = "visualInspection." + i + ".observations";
            if (isBlank(row.getObservations())) {
                errors.put(obsPath, "observations is required");
            } else if (!ALPHA_NUM_PATTERN.matcher(row.getObservations().trim()).matches()) {
                errors.put(obsPath, "observations is invalid");
            }

            if (!isBlank(row.getRemark()) && !ALPHA_NUM_PATTERN.matcher(row.getRemark().trim()).matches()) {
                errors.put("visualInspection." + i + ".remark", "remark is invalid");
            }

            List<VisualInspectionSubItem> subItems = row.getSubItems();
            if (subItems == null) {
                continue;
            }
            for (int j = 0; j < subItems.size(); j++) {
                VisualInspectionSubItem sub = subItems.get(j);
                String subObsPath = "visualInspection." + i + ".subItems." + j + ".observations";
                if (sub == null || isBlank(sub.getObservations())) {
                    errors.put(subObsPath, "observations is required");
                } else if (!ALPHA_NUM_PATTERN.matcher(sub.getObservations().trim()).matches()) {
                    errors.put(subObsPath, "observations is invalid");
                }
                if (sub != null && !isBlank(sub.getRemark())
                        && !ALPHA_NUM_PATTERN.matcher(sub.getRemark().trim()).matches()) {
                    errors.put("visualInspection." + i + ".subItems." + j + ".remark", "remark is invalid");
                }
            }
        }
    }

    private void validateWeightmentForSubmit(Weightment weightment, Map<String, String> errors) {
        if (weightment == null) {
            errors.put("weightment", "weightment is required");
            return;
        }

        if (weightment.getWeightWithoutHarness() == null
                || weightment.getWeightWithoutHarness().getValue() == null) {
            errors.put("weightWithoutHarness", "weightWithoutHarness is required");
        }
        if (weightment.getWeightWithHarness() == null
                || weightment.getWeightWithHarness().getValue() == null) {
            errors.put("weightWithHarness", "weightWithHarness is required");
        }

        WeighscaleCalibration calibration = weightment.getWeighscaleCalibration();
        if (calibration == null) {
            errors.put("weighscaleEquipment", "weighscaleEquipment is required");
            errors.put("calibrationDueDate", "calibrationDueDate is required");
            return;
        }

        if (isBlank(calibration.getEquipmentDetails())) {
            errors.put("weighscaleEquipment", "weighscaleEquipment is required");
        } else if (!ALPHA_NUM_PATTERN.matcher(calibration.getEquipmentDetails().trim()).matches()) {
            errors.put("weighscaleEquipment", "weighscaleEquipment is invalid");
        }
        if (calibration.getCalibrationDueDate() == null) {
            errors.put("calibrationDueDate", "calibrationDueDate is required");
        } else if (calibration.getCalibrationDueDate().isBefore(LocalDate.now())) {
            errors.put("calibrationDueDate", "calibrationDueDate cannot be in the past");
        }
    }

    private void validateDimensionalInspectionForSubmit(
            List<DimensionalInspection> rows,
            Map<String, String> errors) {
        if (rows == null || rows.isEmpty()) {
            errors.put("dimensionalData", "dimensionalData is required");
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            DimensionalInspection row = rows.get(i);
            if (row == null) {
                errors.put("dimensionalData[" + i + "]", "dimensional row is required");
                continue;
            }

            if (isLooseFlapRow(row)) {
                LooseFlap looseFlap = row.getLooseFlap();
                String arcPath = "dimensionalData." + i + ".looseFlap.arcLength";
                String axialPath = "dimensionalData." + i + ".looseFlap.axialLength";
                if (looseFlap == null || looseFlap.getArcLength() == null) {
                    errors.put(arcPath, "arcLength is required");
                } else if (isOutOfRange(looseFlap.getArcLength(), row.getSpecifiedDimension())) {
                    errors.put(arcPath, "arcLength is out of range");
                }
                if (looseFlap == null || looseFlap.getAxialLength() == null) {
                    errors.put(axialPath, "axialLength is required");
                } else if (isOutOfRange(looseFlap.getAxialLength(), row.getSpecifiedDimension())) {
                    errors.put(axialPath, "axialLength is out of range");
                }
                continue;
            }

            Readings readings = row.getReadings();
            validateDimensionalReading(readings == null ? null : readings.getR2tR2b(),
                    "dimensionalData." + i + ".readings.r2tR2b", row.getSpecifiedDimension(), errors);
            validateDimensionalReading(readings == null ? null : readings.getR1rR1l(),
                    "dimensionalData." + i + ".readings.r1rR1l", row.getSpecifiedDimension(), errors);
            validateDimensionalReading(readings == null ? null : readings.getTlBr(),
                    "dimensionalData." + i + ".readings.tlBr", row.getSpecifiedDimension(), errors);
            validateDimensionalReading(readings == null ? null : readings.getTrBl(),
                    "dimensionalData." + i + ".readings.trBl", row.getSpecifiedDimension(), errors);
        }
    }

    private void validateDimensionalReading(
            Double value,
            String path,
            SpecifiedDimension specifiedDimension,
            Map<String, String> errors) {
        if (value == null) {
            errors.put(path, "reading is required");
            return;
        }
        if (isOutOfRange(value, specifiedDimension)) {
            errors.put(path, "reading is out of range");
        }
    }

    private void validateMockTrialForSubmit(MockTrial mockTrial, Map<String, String> errors) {
        if (mockTrial == null) {
            errors.put("mockTrial", "mockTrial is required");
            return;
        }

        MockTrialBasicDetails basic = mockTrial.getBasicDetails();
        if (basic == null) {
            errors.put("mockTrial.castingStation", "castingStation is required");
            errors.put("mockTrial.mandrelId", "mandrelId is required");
        } else {
            if (isBlank(basic.getCastingStation())) {
                errors.put("mockTrial.castingStation", "castingStation is required");
            } else if (!ALPHA_NUM_PATTERN.matcher(basic.getCastingStation().trim()).matches()) {
                errors.put("mockTrial.castingStation", "castingStation is invalid");
            }
            if (isBlank(basic.getMandrelId())) {
                errors.put("mockTrial.mandrelId", "mandrelId is required");
            } else if (!ALPHA_NUM_PATTERN.matcher(basic.getMandrelId().trim()).matches()) {
                errors.put("mockTrial.mandrelId", "mandrelId is invalid");
            }
            if (!isBlank(basic.getBottomCupId()) && !ALPHA_NUM_PATTERN.matcher(basic.getBottomCupId().trim()).matches()) {
                errors.put("mockTrial.bottomCupId", "bottomCupId is invalid");
            }
        }

        List<MotorLengthMeasurement> motorDimensions = mockTrial.getMotorDimensions();
        if (motorDimensions == null || motorDimensions.isEmpty()) {
            errors.put("mockTrial.motorDimensions", "motorDimensions are required");
        } else {
            for (int i = 0; i < motorDimensions.size(); i++) {
                MotorLengthMeasurement row = motorDimensions.get(i);
                validateMockTrialMeasurement(row == null ? null : row.getHeBossWidthWithoutLfRubber(),
                        "mockTrial.motorDimensions." + i + ".heBossWidthWithoutLfRubber", true, errors);
                validateMockTrialMeasurement(row == null ? null : row.getHeDiaId(),
                        "mockTrial.motorDimensions." + i + ".heDiaId", true, errors);
                validateMockTrialMeasurement(row == null ? null : row.getLfRubberThicknessHe(),
                        "mockTrial.motorDimensions." + i + ".lfRubberThicknessHe", false, errors);
                if (row != null && row.getMotorLength() != null) {
                    validateMockTrialMeasurement(row.getMotorLength().getHeOuterToNeOuter(),
                            "mockTrial.motorDimensions." + i + ".heOuterToNeOuter", false, errors);
                    validateMockTrialMeasurement(row.getMotorLength().getHeInnerToNeInner(),
                            "mockTrial.motorDimensions." + i + ".heInnerToNeInner", false, errors);
                    validateMockTrialMeasurement(row.getMotorLength().getNeOuterToHeInner(),
                            "mockTrial.motorDimensions." + i + ".neOuterToHeInner", false, errors);
                }
            }
        }

        List<MandrelAssemblyMeasurement> mandrelRows = mockTrial.getMandrelAssemblyMeasurements();
        if (mandrelRows == null || mandrelRows.isEmpty()) {
            errors.put("mockTrial.mandrelAssemblyMeasurements", "mandrelAssemblyMeasurements are required");
        } else {
            for (int i = 0; i < mandrelRows.size(); i++) {
                MandrelAssemblyMeasurement row = mandrelRows.get(i);
                validateMandrelAssemblyValue(row == null ? null : row.getMandrelRestOnDomeA(),
                        "mockTrial.mandrelAssemblyMeasurements." + i + ".mandrelRestOnDomeA", errors);
                validateMandrelAssemblyValue(row == null ? null : row.getMandrelRestOnBottomCupB(),
                        "mockTrial.mandrelAssemblyMeasurements." + i + ".mandrelRestOnBottomCupB", errors);
                validateMandrelAssemblyValue(row == null ? null : row.getBellowThicknessD(),
                        "mockTrial.mandrelAssemblyMeasurements." + i + ".bellowThicknessD", errors);
            }
        }
    }

    private void validateMockTrialMeasurement(
            MockTrialMeasurementValue measurement,
            String path,
            boolean required,
            Map<String, String> errors) {
        if (measurement == null || measurement.getValue() == null) {
            if (required) {
                errors.put(path, "value is required");
            }
            return;
        }
    }

    private void validateMandrelAssemblyValue(BigDecimal value, String path, Map<String, String> errors) {
        if (value == null) {
            errors.put(path, "value is required");
        }
    }

    private void validateReportUploadForSubmit(ReportUploadDto reports, Map<String, String> errors) {
        if (reports == null) {
            errors.put("visualInspectionReport", "visualInspectionReport is required");
            errors.put("dimensionalInspectionReport", "dimensionalInspectionReport is required");
            errors.put("mockTrialReport", "mockTrialReport is required");
            return;
        }

        if (!hasReadyDocument(reports.getVisualInspectionReport())) {
            errors.put("visualInspectionReport", "visualInspectionReport is required");
        }
        if (!hasReadyDocument(reports.getDimensionalInspectionReport())) {
            errors.put("dimensionalInspectionReport", "dimensionalInspectionReport is required");
        }
        if (!hasReadyDocument(reports.getMockTrialReport())) {
            errors.put("mockTrialReport", "mockTrialReport is required");
        }
    }

    private void validateRadiographyForSubmit(RadiographyDetails radiography, Map<String, String> errors) {
        if (radiography == null || isBlank(radiography.getRadiographyPlanName())) {
            errors.put("radiographyPlanName", "radiographyPlanName is required");
        } else if (radiography.getRadiographyPlanName().trim().length() > MAX_STANDARD_FIELD_LENGTH) {
            errors.put(
                    "radiographyPlanName",
                    "radiographyPlanName cannot exceed " + MAX_STANDARD_FIELD_LENGTH + " characters");
        }

        List<RadiographyPlanDetailsDto> rows =
                radiography != null ? radiography.getRadiographyPlanDetails() : null;
        if (rows == null || rows.isEmpty()) {
            errors.put("radiographyPlanRows", "At least one radiography plan row is required");
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            RadiographyPlanDetailsDto row = rows.get(i);
            if (row == null) {
                errors.put("radiographyPlanRows." + i + ".sections", "sections is required");
                continue;
            }
            if (row.getNumberOfSections() == null) {
                errors.put("radiographyPlanRows." + i + ".sections", "sections is required");
            }
            if (row.getNumberOfOrientations() == null) {
                errors.put("radiographyPlanRows." + i + ".orientations", "orientations is required");
            }
            if (row.getSfd() == null) {
                errors.put("radiographyPlanRows." + i + ".sfd", "sfd is required");
            }
            if (row.getNumberOfNormalExposures() == null) {
                errors.put("radiographyPlanRows." + i + ".normalExposures", "normalExposures is required");
            }
            if (row.getNumberOfTangentialExposures() == null) {
                errors.put(
                        "radiographyPlanRows." + i + ".tangentialExposures",
                        "tangentialExposures is required");
            }
            if (isBlank(row.getDetectorType())) {
                errors.put("radiographyPlanRows." + i + ".detectorType", "detectorType is required");
            }
        }
    }

    private boolean hasReadyDocument(List<DocumentDto> docs) {
        return docs != null && docs.stream()
                .anyMatch(doc -> doc != null && !isBlank(doc.getFileId()));
    }

    private boolean isLooseFlapRow(DimensionalInspection row) {
        if (row.getLooseFlap() != null
                && (row.getLooseFlap().getArcLength() != null || row.getLooseFlap().getAxialLength() != null)) {
            return true;
        }
        return String.valueOf(row.getParamName()).toLowerCase(Locale.ROOT).contains("loose flap");
    }

    private boolean isOutOfRange(Double value, SpecifiedDimension specifiedDimension) {
        if (value == null || specifiedDimension == null) {
            return false;
        }
        Double min = specifiedDimension.getMinValue();
        Double max = specifiedDimension.getMaxValue();
        if (min == null && max == null) {
            return false;
        }
        if (min != null && value < min) {
            return true;
        }
        return max != null && value > max;
    }

    private boolean isOutOfRange(Double value, in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange referenceRange) {
        if (value == null || referenceRange == null) {
            return false;
        }
        Double min = referenceRange.getMinValue();
        Double max = referenceRange.getMaxValue();
        if (min == null && max == null) {
            return false;
        }
        if (min != null && value < min) {
            return true;
        }
        return max != null && value > max;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void throwValidationErrors(Map<String, String> errors) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        throw new AcemException(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
    }

    private List<String> promoteSectionFiles(
            Sections sections,
            Long subDepartmentId,
            String motorCasingId,
            Set<String> existingFileIds) {

        List<String> promotedFileIds = new ArrayList<>();
        if (sections == null) {
            return promotedFileIds;
        }

        // 1. Insulation report file
        if (sections.getMotorReceipt() != null && sections.getMotorReceipt().getInsulation() != null) {
            DocumentDto insulationMedia = sections.getMotorReceipt().getInsulation().getReportUpload();
            promoteMediaFile(insulationMedia, subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
        }

        // 2. Visual inspection files
        if (sections.getVisualInspection() != null) {
            for (VisualInspection item : sections.getVisualInspection()) {
                promoteMediaFile(item.getMedia(), subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
            }
        }

        // 3. Report upload lists
        if (sections.getReportUpload() != null) {
            ReportUploadDto reports = sections.getReportUpload();
            promoteDocumentList(reports.getNdtUtReport(), subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
            promoteDocumentList(reports.getVisualInspectionReport(), subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
            promoteDocumentList(reports.getWeighmentReport(), subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
            promoteDocumentList(reports.getDimensionalInspectionReport(), subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
            promoteDocumentList(reports.getMockTrialReport(), subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
            promoteDocumentList(reports.getInsulationLiningReport(), subDepartmentId, motorCasingId, existingFileIds, promotedFileIds);
        }

        return promotedFileIds;
    }

    private void promoteMediaFile(
            DocumentDto media,
            Long subDepartmentId,
            String motorCasingId,
            Set<String> existingFileIds,
            List<String> promotedFileIds) {

        if (media != null && media.getFileId() != null && !media.getFileId().isBlank()) {
            String fileId = media.getFileId().trim();

            // SCENARIO 1: File already exists in DB -> Enrich DocumentDto with existing metadata
            if (existingFileIds.contains(fileId)) {
                fileMetadataEnrichmentService.enrichDocument(media, subDepartmentId);
                return;
            }

            // SCENARIO 2: New File -> Promote & Bind
            try {
                FileAttachments savedFile = fileBindingService.promoteAndBindFile(
                        fileId,
                        subDepartmentId,
                        motorCasingId,
                        FileBindingScope.forMotorCasing(motorCasingId)
                );

                media.setFileId(savedFile.getFileId());
                media.setFileName(savedFile.getOriginalFileName());
                media.setFileUrl(savedFile.getFilePath());
                media.setStatus(savedFile.getStatus());
                media.setMimeType(savedFile.getMimeType());
                media.setReadOnly(savedFile.isReadOnly());

                promotedFileIds.add(savedFile.getFileId());
            } catch (Exception e) {
                log.error("File promotion failed for media fileId={}", fileId, e);
                throw new AcemException(
                        ErrorCode.FILE_TRANSFER_ERROR,
                        "File upload failed, please try again."
                );
            }
        }
    }

    private void promoteDocumentList(
            List<DocumentDto> docs,
            Long subDepartmentId,
            String motorCasingId,
            Set<String> existingFileIds,
            List<String> promotedFileIds) {

        if (docs == null || docs.isEmpty()) {
            return;
        }

        for (DocumentDto doc : docs) {
            if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
                String fileId = doc.getFileId().trim();

                // SCENARIO 1: File already exists in DB -> Enrich DocumentDto with existing metadata
                if (existingFileIds.contains(fileId)) {
                    fileMetadataEnrichmentService.enrichDocument(doc, subDepartmentId);
                    continue;
                }

                // SCENARIO 2: New File -> Promote & Bind
                try {
                    FileAttachments savedFile = fileBindingService.promoteAndBindFile(
                            fileId,
                            subDepartmentId,
                            motorCasingId,
                            FileBindingScope.forMotorCasing(motorCasingId)
                    );

                    doc.setFileId(savedFile.getFileId());
                    doc.setFileName(savedFile.getOriginalFileName());
                    doc.setFileUrl(savedFile.getFilePath());
                    doc.setStatus(savedFile.getStatus());
                    doc.setMimeType(savedFile.getMimeType());
                    doc.setReadOnly(savedFile.isReadOnly());

                    promotedFileIds.add(savedFile.getFileId());
                } catch (Exception e) {
                    log.error("File promotion failed for document fileId={}", fileId, e);
                    throw new AcemException(
                            ErrorCode.FILE_TRANSFER_ERROR,
                            "File upload failed, please try again."
                    );
                }
            }
        }
    }

    /** Live MDM append only while the form is still TO_BE_INITIATED. */
    private static boolean isMasterRowFrozen(Status status) {
        return status != null && status != Status.TO_BE_INITIATED;
    }

    /**
     * Overlay master specificationName + referenceRange onto stored insulation params for details.
     * Persist shape stays lean (code + reported + acemSpec); this is read-path only.
     * Once past TO_BE_INITIATED, keep saved rows and do not append later master parameters.
     */
    private void enrichInsulationSpecificationMetadata(Sections sections, boolean freezeNewRows) {
        if (sections == null || sections.getMotorReceipt() == null) {
            return;
        }
        Insulation insulation = sections.getMotorReceipt().getInsulation();
        if (insulation == null) {
            return;
        }

        String type = insulation.getType();
        if (isBlank(type) && insulation.getInsulationSpecification() != null) {
            type = insulation.getInsulationSpecification().getInsulationType();
        }
        if (isBlank(type)) {
            return;
        }

        InsulationSpecification master = insulationSpecificationRepository
                .findByInsulationTypeIgnoreCase(type)
                .orElse(null);
        if (master == null || !InsulationSpecificationMasterDataService.isActive(master)
                || master.getSpecifications() == null || master.getSpecifications().isEmpty()) {
            return;
        }

        Map<String, ParameterDto> masterByCode = new LinkedHashMap<>();
        for (CategoryDto masterCategory : InsulationSpecificationMasterDataService.filterActiveSpecifications(
                master.getSpecifications())) {
            if (masterCategory == null || masterCategory.getParameters() == null) {
                continue;
            }
            for (ParameterDto masterParam : masterCategory.getParameters()) {
                if (masterParam == null || isBlank(masterParam.getSpecificationCode())) {
                    continue;
                }
                masterByCode.putIfAbsent(masterParam.getSpecificationCode().trim(), masterParam);
            }
        }

        Map<String, SpecificationParameterDto> savedByCode = new LinkedHashMap<>();
        InsulationSpecificationDto stored = insulation.getInsulationSpecification();
        if (stored != null && stored.getSpecifications() != null) {
            stored.getSpecifications().stream()
                    .filter(Objects::nonNull)
                    .map(SpecificationCategoryDto::getParameters)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .filter(param -> !isBlank(param.getSpecificationCode()))
                    .forEach(param -> savedByCode.putIfAbsent(param.getSpecificationCode().trim(), param));
        }

        if (freezeNewRows && stored != null && stored.getSpecifications() != null) {
            List<SpecificationCategoryDto> frozenCategories = new ArrayList<>();
            for (SpecificationCategoryDto storedCategory : stored.getSpecifications()) {
                if (storedCategory == null) {
                    continue;
                }
                List<SpecificationParameterDto> frozenParams = new ArrayList<>();
                if (storedCategory.getParameters() != null) {
                    for (SpecificationParameterDto saved : storedCategory.getParameters()) {
                        if (saved == null || isBlank(saved.getSpecificationCode())) {
                            continue;
                        }
                        String code = saved.getSpecificationCode().trim();
                        ParameterDto masterParam = masterByCode.get(code);
                        frozenParams.add(SpecificationParameterDto.builder()
                                .specificationCode(code)
                                .specificationName(masterParam != null
                                        ? masterParam.getSpecificationName()
                                        : saved.getSpecificationName())
                                .referenceRange(masterParam != null
                                        ? masterParam.getReferenceRange()
                                        : saved.getReferenceRange())
                                .reported(saved.getReported())
                                .acemSpec(saved.getAcemSpec())
                                .build());
                    }
                }
                frozenCategories.add(SpecificationCategoryDto.builder()
                        .category(storedCategory.getCategory())
                        .parameters(frozenParams)
                        .build());
            }
            insulation.setInsulationSpecification(InsulationSpecificationDto.builder()
                    .insulationType(type)
                    .specifications(frozenCategories)
                    .build());
            return;
        }

        List<SpecificationCategoryDto> enrichedCategories = new ArrayList<>();
        Set<String> masterCodes = new LinkedHashSet<>();

        for (CategoryDto masterCategory : InsulationSpecificationMasterDataService.filterActiveSpecifications(
                master.getSpecifications())) {
            if (masterCategory == null) {
                continue;
            }
            List<SpecificationParameterDto> enrichedParams = new ArrayList<>();
            if (masterCategory.getParameters() != null) {
                for (ParameterDto masterParam : masterCategory.getParameters()) {
                    if (masterParam == null || isBlank(masterParam.getSpecificationCode())) {
                        continue;
                    }
                    String code = masterParam.getSpecificationCode().trim();
                    masterCodes.add(code);
                    SpecificationParameterDto saved = savedByCode.get(code);
                    enrichedParams.add(SpecificationParameterDto.builder()
                            .specificationCode(code)
                            .specificationName(masterParam.getSpecificationName())
                            .referenceRange(masterParam.getReferenceRange())
                            .reported(saved != null ? saved.getReported() : null)
                            .acemSpec(saved != null ? saved.getAcemSpec() : null)
                            .build());
                }
            }
            enrichedCategories.add(SpecificationCategoryDto.builder()
                    .category(masterCategory.getCategory())
                    .parameters(enrichedParams)
                    .build());
        }

        // Keep any stored codes not present in master (values only; no master metadata).
        List<SpecificationParameterDto> unknownParams = savedByCode.entrySet().stream()
                .filter(entry -> !masterCodes.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (!unknownParams.isEmpty()) {
            enrichedCategories.add(SpecificationCategoryDto.builder()
                    .category("Other")
                    .parameters(unknownParams)
                    .build());
        }

        InsulationSpecificationDto enriched = InsulationSpecificationDto.builder()
                .insulationType(type)
                .specifications(enrichedCategories)
                .build();
        insulation.setInsulationSpecification(enriched);
    }

    private void enrichSectionFiles(Sections sections, Long subDepartmentId) {
        if (sections == null) {
            return;
        }

        if (sections.getMotorReceipt() != null && sections.getMotorReceipt().getInsulation() != null) {
            fileMetadataEnrichmentService.enrichDocument(
                    sections.getMotorReceipt().getInsulation().getReportUpload(),
                    subDepartmentId);
        }

        if (sections.getVisualInspection() != null) {
            for (VisualInspection item : sections.getVisualInspection()) {
                if (item != null) {
                    fileMetadataEnrichmentService.enrichDocument(item.getMedia(), subDepartmentId);
                }
            }
        }

        if (sections.getReportUpload() != null) {
            ReportUploadDto reports = sections.getReportUpload();
            fileMetadataEnrichmentService.enrichDocuments(reports.getNdtUtReport(), subDepartmentId);
            fileMetadataEnrichmentService.enrichDocuments(reports.getVisualInspectionReport(), subDepartmentId);
            fileMetadataEnrichmentService.enrichDocuments(reports.getWeighmentReport(), subDepartmentId);
            fileMetadataEnrichmentService.enrichDocuments(reports.getDimensionalInspectionReport(), subDepartmentId);
            fileMetadataEnrichmentService.enrichDocuments(reports.getMockTrialReport(), subDepartmentId);
            fileMetadataEnrichmentService.enrichDocuments(reports.getInsulationLiningReport(), subDepartmentId);
        }
    }

    private void rollbackPromotedFiles(List<String> promotedFileIds) {
        if (promotedFileIds == null || promotedFileIds.isEmpty()) {
            return;
        }
        log.info("Starting rollback for {} promoted files...", promotedFileIds.size());
        for (String fileId : promotedFileIds) {
            if (fileId != null && !fileId.isBlank()) {
                try {
                    fileBindingService.rollbackOrUnbindFile(fileId);
                } catch (Exception e) {
                    log.error("Failed to execute file rollback for fileId={}", fileId, e);
                }
            }
        }
    }

    private Set<String> extractAllFileIds(Sections sections) {
        Set<String> fileIds = new HashSet<>();
        if (sections == null) {
            return fileIds;
        }

        // 1. Insulation report file
        if (sections.getMotorReceipt() != null && sections.getMotorReceipt().getInsulation() != null) {
            DocumentDto insulationMedia = sections.getMotorReceipt().getInsulation().getReportUpload();
            if (insulationMedia != null && insulationMedia.getFileId() != null && !insulationMedia.getFileId().isBlank()) {
                fileIds.add(insulationMedia.getFileId());
            }
        }

        // 2. Visual inspection files
        if (sections.getVisualInspection() != null) {
            for (VisualInspection item : sections.getVisualInspection()) {
                if (item != null && item.getMedia() != null && item.getMedia().getFileId() != null && !item.getMedia().getFileId().isBlank()) {
                    fileIds.add(item.getMedia().getFileId());
                }
            }
        }

        // 3. Report upload lists
        if (sections.getReportUpload() != null) {
            ReportUploadDto reports = sections.getReportUpload();
            addDocumentFileIds(reports.getNdtUtReport(), fileIds);
            addDocumentFileIds(reports.getVisualInspectionReport(), fileIds);
            addDocumentFileIds(reports.getWeighmentReport(), fileIds);
            addDocumentFileIds(reports.getDimensionalInspectionReport(), fileIds);
            addDocumentFileIds(reports.getMockTrialReport(), fileIds);
            addDocumentFileIds(reports.getInsulationLiningReport(), fileIds);
        }

        return fileIds;
    }

    private void addDocumentFileIds(List<DocumentDto> docs, Set<String> fileIds) {
        if (docs != null) {
            for (DocumentDto doc : docs) {
                if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
                    fileIds.add(doc.getFileId());
                }
            }
        }
    }

}
