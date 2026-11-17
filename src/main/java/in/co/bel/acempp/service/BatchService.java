package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.batch.*;
import in.co.bel.acempp.dto.general.*;
import in.co.bel.acempp.dto.casepreparation.MotorStatusResponse;
import in.co.bel.acempp.dto.subscale.SubScaleMetadata;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.RadiographyDetails;
import in.co.bel.acempp.dto.rocketmotorcasing.RocketMotorListdata;
import in.co.bel.acempp.dto.rocketmotorcasing.RocketMotorMetadata;
import in.co.bel.acempp.dto.mixing.MixingMetadata;
import in.co.bel.acempp.dto.rawmaterialpreparation.*;
import in.co.bel.acempp.dto.qcdivision.QcStageDivisionStatusDto;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.Insulation;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.MotorReceipt;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.Sections;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.entity.*;
import in.co.bel.acempp.entity.batch.*;
import in.co.bel.acempp.entity.manufacturing.*;
import in.co.bel.acempp.entity.sourcing.Lot;
import in.co.bel.acempp.entity.sourcing.RocketMotorCasing;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.RmpFormTemplate;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.*;
import in.co.bel.acempp.repository.sourcing.LotRepository;
import in.co.bel.acempp.repository.sourcing.RocketMotorCasingRepository;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionStageStatusResolver;
import in.co.bel.acempp.service.qualitycontrol.QcDivisionCatalog;
import in.co.bel.acempp.service.batch.MotorBatchLifecycleHelper;
import in.co.bel.acempp.service.batch.StageProgressDisplayEnricher;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mongodb.client.AggregateIterable;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private static final long STATIC_TESTING_SUB_DEPARTMENT_ID = 12L;
    private static final String BATCH_COLLECTION = "batches";
    private static final String BATCH_ID_FIELD = "batchId";
    private static final String BATCH_ID_PREFIX = "ACEM";
    private static final String BATCH_ID_COUNTER_ID = "batches:batchId:ACEM";
    private static final Pattern BATCH_ID_PATTERN =
            Pattern.compile("^ACEM/\\d{4}/\\d+$");

    private final DepartmentRepository departmentRepo;
    private final BatchRepository batchRepository;
    private final ProjectRepository projectRepository;
    private final LotRepository lotRepository;
    private final UserRepository userRepository;
    private final BatchStageSequence batchStageSequence;
    private final RocketMotorCasingRepository rmcRepository;
    private final MaterialRepository materialRepository;
    private final MixerMasterRepository mixerMasterRepository;
    private final SubscaleArticleRepository subscaleArticleRepository;
    private final BatchStageOrchestrationService stageOrchestrationService;
    private final SubscaleMetadataResolver subscaleMetadataResolver;
    private final MixingMetadataResolver mixingMetadataResolver;
    private final QcDivisionStageStatusResolver qcDivisionStatusResolver;
    private final StageProgressDisplayEnricher stageProgressDisplayEnricher;
    private final MongoTemplate mongoTemplate;


    long allocated;
    long approved;
    long rejected;
    long pending;


    //    --------------------------CREATE BATCH----------------------------------------------------------------------------------
    public BatchResponseData createBatch(BatchRequest request, String userId) {

        User creator = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.USER_NOT_FOUND,
                        "Creator not found"));

        boolean isSubscale = "SUBSCALE".equalsIgnoreCase(request.getBatchType());
        User sysMgr = null;
        if (!isSubscale || (request.getSystemManagerId() != null && !request.getSystemManagerId().isBlank())) {
            if (request.getSystemManagerId() == null || request.getSystemManagerId().isBlank()) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "System Manager is required for main batches");
            }
            sysMgr = userRepository.findByUserId(request.getSystemManagerId())
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.DEPENDENCY_NOT_FOUND,
                            "System Manager not found: " + request.getSystemManagerId()));
        }

        validateRequest(request);
        validateReferences(request);
        validateMotorIds(request);

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findByProjectId(request.getProjectId())
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.DEPENDENCY_NOT_FOUND,
                            "Project not found: " + request.getProjectId()));
        }
        String identificationSheetStatus =
                computeIdentificationSheetStatus(request.getIdentificationSheet());

        String batchId = generateBatchId();

        boolean parallelMainFlow = batchStageSequence.getInitialStages(
                request.getBatchType(), true).size() > 1
                && !"SUBSCALE".equalsIgnoreCase(request.getBatchType());

        List<SubscaleArticleInfo> articleInfos = null;

        if ("EXPERIMENTAL".equalsIgnoreCase(request.getSubBatchType())
                && request.getArticles() != null
                && !request.getArticles().isEmpty()) {

            List<String> codes = request.getArticles()
                    .stream()
                    .map(SubscaleArticleRequest::getSubscaleArticleCode)
                    .toList();

            List<SubscaleArticleMaster> masters =
                    subscaleArticleRepository.findBySubscaleArticleCodeIn(codes);

            if (masters.size() != codes.size()) {
                throw new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "One or more subscale articles not found"
                );
            }

            articleInfos = masters.stream()
                    .map(article -> SubscaleArticleInfo.builder()
                            .subscaleArticleId(article.getSubscaleArticleId())
                            .subscaleArticleCode(article.getSubscaleArticleCode())
                            .subscaleArticleName(article.getSubscaleArticleName())
                            .isBem(Boolean.TRUE.equals(article.getIsBem()))
                            .isActive(article.getIsActive())
                            .build())
                    .toList();
        }

        Batch batch = Batch.builder()
                .batchId(batchId)
                .batchType(request.getBatchType().toUpperCase())
                .subBatchType(request.getSubBatchType() != null
                        ? request.getSubBatchType().toUpperCase()
                        : null)
                .projectId(request.getProjectId())
                .projectName(project != null ? project.getProjectName() : null)
                .motorStage(request.getMotorStage())
                .numberOfMotors(request.getNumberOfMotors())
                .motorIds(request.getMotorIds())
                .articles(articleInfos)
                .objective(request.getObjective())
                .systemManager(sysMgr != null ? PersonInfo.builder()
                        .id(sysMgr.getUserId())
                        .fullName(sysMgr.getUsername())
                        .build() : null)
                .identificationSheet(buildIdentificationSheet(request.getIdentificationSheet()))
                .identificationSheetStatus(identificationSheetStatus)
                .parallelFlowEnabled(parallelMainFlow ? Boolean.TRUE : Boolean.FALSE)
                .createdBy(PersonInfo.builder()
                        .id(userId)
                        .fullName(creator.getUsername())
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        // Add first stage(s) only if Identification Sheet is completed
        if ("COMPLETED".equalsIgnoreCase(identificationSheetStatus)) {
            List<Long> initialStages = batchStageSequence.getInitialStages(
                    request.getBatchType(),
                    Boolean.TRUE.equals(batch.getParallelFlowEnabled()));
            stageOrchestrationService.createInitialStages(
                    batch,
                    initialStages,
                    LocalDateTime.now());
        } else {
            setYetToAssignStage(batch, batch.getCreatedAt());
        }
        batch = batchRepository.save(batch);
        updateRocketMotorMetadata(batch.getBatchId(), request.getMotorIds());


        log.info("Batch {} created by {}", batchId, userId);

        return BatchResponseData.builder()
                .batchId(batch.getBatchId())
                .batchType(batch.getBatchType())
                .subBatchType(batch.getSubBatchType())
                .projectId(batch.getProjectId())
                .systemManagerId(sysMgr != null ? sysMgr.getUserId() : null)
                .identificationSheetStatus(batch.getIdentificationSheetStatus())
                .currentStage(stageProgressDisplayEnricher.enrichCopies(
                        batch, currentStagesForDisplay(batch)))
                .createdOn(batch.getCreatedAt())
                .build();
    }
//    --------------------------UPDATE BATCH----------------------------------------------------------------------------------

    public UpdateBatchResponse updateBatch(BatchRequest request, String userId) {

        // 1. Find existing batch
        Batch batch = batchRepository.findByBatchId(request.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists with batchId: " + request.getBatchId()));
        if (batch.isDeleted()) {
            throw new AcemException(ErrorCode.BATCH_NOT_FOUND,
                    "Batch not found");
        }
        User updater = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "Updater not found"));

        boolean isSubscale = "SUBSCALE".equalsIgnoreCase(request.getBatchType());
        User sysMgr = null;
        if (!isSubscale || (request.getSystemManagerId() != null && !request.getSystemManagerId().isBlank())) {
            if (request.getSystemManagerId() == null || request.getSystemManagerId().isBlank()) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "System Manager is required for main batches");
            }
            sysMgr = userRepository.findByUserId(request.getSystemManagerId())
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.DEPENDENCY_NOT_FOUND,
                            "System Manager not found: " + request.getSystemManagerId()));
        }
        // 2. Basic request validation (same as create)
        validateUpdateRequest(request);

        // 3. Validate references (systemManager, project, lots)
        validateReferencesForUpdate(request);

        validateMotorIdsForUpdate(request);
        if (request.getProjectId() != null) {
            Project project = projectRepository.findByProjectId(request.getProjectId())
                    .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                            "Project not found: " + request.getProjectId()));
            batch.setProjectId(request.getProjectId());
            batch.setProjectName(project.getProjectName());
        }
        // 4. Update top‑level fields (full replacement)
        if (request.getArticles() != null) {

            List<String> codes = request.getArticles()
                    .stream()
                    .map(SubscaleArticleRequest::getSubscaleArticleCode)
                    .toList();

            List<SubscaleArticleMaster> masters =
                    subscaleArticleRepository.findBySubscaleArticleCodeIn(codes);

            if (masters.size() != codes.size()) {
                throw new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "One or more subscale articles not found"
                );
            }

            batch.setArticles(
                    masters.stream()
                            .map(article -> SubscaleArticleInfo.builder()
                                    .subscaleArticleId(article.getSubscaleArticleId())
                                    .subscaleArticleCode(article.getSubscaleArticleCode())
                                    .subscaleArticleName(article.getSubscaleArticleName())
                                    .isBem(Boolean.TRUE.equals(article.getIsBem()))
                                    .isActive(article.getIsActive())
                                    .build())
                            .toList()
            );
        }
        batch.setBatchType(request.getBatchType().toUpperCase());
        batch.setSubBatchType(request.getSubBatchType() != null
                ? request.getSubBatchType().toUpperCase() : null);
        batch.setMotorStage(request.getMotorStage());
        batch.setNumberOfMotors(request.getNumberOfMotors());
        batch.setMotorIds(request.getMotorIds());
        batch.setObjective(request.getObjective());
        batch.setSystemManager(sysMgr != null ? PersonInfo.builder()
                .id(sysMgr.getUserId())
                .fullName(sysMgr.getUsername())
                .build() : null);

        // 5. Update identification sheet (if provided)
        if (request.getIdentificationSheet() != null) {

            IdentificationSheetDto sheetRequest = request.getIdentificationSheet();
            // Preserve mixer when edit payload omits it (legacy sheets / incomplete round-trip).
            if (isBlank(resolveMixerTypeCode(sheetRequest.getMixerType()))
                    && batch.getIdentificationSheet() != null
                    && batch.getIdentificationSheet().getMixerType() != null) {
                sheetRequest.setMixerType(batch.getIdentificationSheet().getMixerType());
            }

            batch.setIdentificationSheet(buildIdentificationSheet(sheetRequest));

            String identificationSheetStatus = computeIdentificationSheetStatus(sheetRequest);
            // Don't regress COMPLETED → DRAFT for legacy sheets that never persisted mixerType.
            if ("DRAFT".equalsIgnoreCase(identificationSheetStatus)
                    && "COMPLETED".equalsIgnoreCase(batch.getIdentificationSheetStatus())
                    && isBlank(resolveMixerTypeCode(sheetRequest.getMixerType()))
                    && sheetCompleteExceptMixer(sheetRequest)) {
                identificationSheetStatus = "COMPLETED";
            }

            batch.setIdentificationSheetStatus(identificationSheetStatus);

            if ("COMPLETED".equals(identificationSheetStatus)
                    && (batch.getStageProgress() == null || batch.getStageProgress().isEmpty())) {

                if (batch.getParallelFlowEnabled() == null
                        && !"SUBSCALE".equalsIgnoreCase(batch.getBatchType())) {
                    batch.setParallelFlowEnabled(Boolean.TRUE);
                }
                List<Long> initialStages = batchStageSequence.getInitialStages(
                        batch.getBatchType(),
                        Boolean.TRUE.equals(batch.getParallelFlowEnabled()));
                batch.setStageProgress(new ArrayList<>());
                stageOrchestrationService.createInitialStages(
                        batch,
                        initialStages,
                        LocalDateTime.now());
            } else if ("DRAFT".equalsIgnoreCase(identificationSheetStatus)
                    && (batch.getStageProgress() == null || batch.getStageProgress().isEmpty())) {
                setYetToAssignStage(batch, LocalDateTime.now());
            }
        }

        if ("DRAFT".equalsIgnoreCase(batch.getIdentificationSheetStatus())
                && (batch.getStageProgress() == null || batch.getStageProgress().isEmpty())
                && (batch.getCurrentStage() == null || batch.getCurrentStage().isEmpty())) {
            setYetToAssignStage(batch, LocalDateTime.now());
        }

        // 6. Audit
        batch.setUpdatedBy(PersonInfo.builder()
                .id(userId)
                .fullName(updater.getUsername())
                .build());
        batch.setUpdatedAt(LocalDateTime.now());

        batch = batchRepository.save(batch);
        updateRocketMotorMetadata(batch.getBatchId(), request.getMotorIds());
        log.info("Batch {} updated by {}", batch.getBatchId(), userId);

        return UpdateBatchResponse.builder()
                .batchId(batch.getBatchId())
                .batchType(batch.getBatchType())
                .subBatchType(batch.getSubBatchType())
                .projectId(batch.getProjectId())
                .systemManagerId(sysMgr != null ? sysMgr.getUserId() : null)
                .identificationSheetStatus(batch.getIdentificationSheetStatus())
                .currentStage(stageProgressDisplayEnricher.enrichCopies(
                        batch, currentStagesForDisplay(batch)))
                .updatedOn(batch.getUpdatedAt())
                .build();
    }

    //    --------------------------GET BATCH DETAILS----------------------------------------------------------------------------------
    public BatchDetailsResponse getBatchDetails(String batchId) {

        // Backfill QC STATIC_TEST_FACILITY unlock / motor units when STF BEM already APPROVED.
        stageOrchestrationService.reconcileStaleUnlocks(batchId);

        final Batch batch = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists with batchId: " + batchId));

        if (batch.isDeleted()) {
            throw new AcemException(
                    ErrorCode.BATCH_NOT_FOUND,
                    "Batch not found");
        }

        BatchMetadata metadata = (batch.getIdentificationSheet() != null && batch.getIdentificationSheet().getMetadata() != null)
                ? batch.getIdentificationSheet().getMetadata()
                : new BatchMetadata();

        SubScaleMetadata approvedSubScale =
                subscaleMetadataResolver.resolveApprovedMetadata(batch.getBatchId());
        SubScaleMetadata anySubScale =
                subscaleMetadataResolver.resolveMetadataForBatch(batch.getBatchId());
        if (approvedSubScale != null) {
            // BEM/hardware stay approval-gated; always overlay mixingCycle for QC Mixing MDM.
            if (approvedSubScale.getMixingCycle() == null && anySubScale != null) {
                approvedSubScale.setMixingCycle(anySubScale.getMixingCycle());
            }
            metadata.setSubScale(approvedSubScale);
        } else if (anySubScale != null && anySubScale.getMixingCycle() != null) {
            metadata.setSubScale(SubScaleMetadata.builder()
                    .mixingCycle(anySubScale.getMixingCycle())
                    .build());
        }

        MixingMetadata approvedMixingMetadata =
                mixingMetadataResolver.resolveApprovedMetadata(batch.getBatchId());
        if (approvedMixingMetadata != null) {
            metadata.setMixing(approvedMixingMetadata);
        }

        enrichMetadataWithRadiography(metadata);

        IdentificationSheetDto sheetDto = null;
        if (batch.getIdentificationSheet() != null) {
            final Map<String, MaterialMaster> materialByCode;
            if (batch.getIdentificationSheet().getMaterials() != null
                    && !batch.getIdentificationSheet().getMaterials().isEmpty()) {
                List<String> materialCodes = batch.getIdentificationSheet()
                        .getMaterials()
                        .stream()
                        .map(BatchMaterial::getMaterialCode)
                        .distinct()
                        .collect(Collectors.toList());

                materialByCode = materialRepository.findByMaterialCodeIn(materialCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                MaterialMaster::getMaterialCode,
                                m -> m,
                                (a, b) -> a
                        ));
            } else {
                materialByCode = Collections.emptyMap();
            }

            List<BatchMaterialDto> materials = batch.getIdentificationSheet().getMaterials() != null
                    ? java.util.stream.IntStream.range(0, batch.getIdentificationSheet().getMaterials().size())
                    .mapToObj(index -> {
                        BatchMaterial m = batch.getIdentificationSheet().getMaterials().get(index);
                        BatchMaterialDto dto = new BatchMaterialDto();
                        // Normalize legacy duplicate/gap srNos on read.
                        dto.setSrNo(index + 1);
                        dto.setMaterialCode(m.getMaterialCode());
                        MaterialMaster master = materialByCode.get(m.getMaterialCode());
                        if (master != null) {
                            dto.setMaterialId(master.getMaterialId());
                            dto.setMaterialName(
                                    master.getMaterialName() != null
                                            ? master.getMaterialName()
                                            : m.getMaterialName());
                            dto.setMaterialType(master.getMaterialType());
                            dto.setRmpFormTemplate(RmpFormTemplate.orDefault(master.getRmpFormTemplate()));
                        } else {
                            dto.setMaterialName(m.getMaterialName());
                        }

                        dto.setGradeCode(
                                m.getGrade() != null
                                        ? m.getGrade().getGradeCode()
                                        : null
                        );
                        dto.setGradeName(
                                m.getGrade() != null
                                        ? m.getGrade().getGradeName()
                                        : null
                        );

                        dto.setLotIds(m.getLotIds());
                        dto.setMake(m.getMake());
                        dto.setRequiredComposition(m.getRequiredComposition());
                        dto.setQuantityPerPremix(m.getQuantityPerPremix());
                        dto.setRevalidationFromDate(m.getRevalidationFromDate());
                        dto.setRevalidationToDate(m.getRevalidationToDate());

                        return dto;
                    })
                    .collect(Collectors.toList())
                    : null;

            sheetDto = IdentificationSheetDto.builder()
                    .date(batch.getIdentificationSheet().getDate())
                    .batchSize(batch.getIdentificationSheet().getBatchSize())
                    .bondingSheetNo(batch.getIdentificationSheet().getBondingSheetNo())
                    .mixerType(batch.getIdentificationSheet().getMixerType())
                    .numberOfPremix(batch.getIdentificationSheet().getNumberOfPremix())
                    .remarks(batch.getIdentificationSheet().getRemarks())
                    .prcApprovalDate(batch.getIdentificationSheet().getPrcApprovalDate())
                    .materials(materials)
                    .metadata(metadata)
                    .build();
        }

        List<QcStageDivisionStatusDto> legacyQcDivisionStatuses =
                resolveLegacyQcDivisionStatuses(batch);
        List<Batch.StageProgress> displayCurrentStages = filterBemMotorStatusesForDisplay(
                currentStagesForDisplay(batch), batch, approvedSubScale, legacyQcDivisionStatuses);
        List<Batch.StageProgress> displayStageProgress = filterBemMotorStatusesForDisplay(
                safeStageProgress(batch), batch, approvedSubScale, legacyQcDivisionStatuses);
        stageProgressDisplayEnricher.enrichBatchViews(
                batch, displayStageProgress, displayCurrentStages);

        return BatchDetailsResponse.builder()
                .batch(BatchDetailDto.builder()
                        .batchId(batch.getBatchId())
                        .batchType(batch.getBatchType())
                        .parallelFlowEnabled(batch.getParallelFlowEnabled())
                        .subBatchType(batch.getSubBatchType())
                        .project(
                                ProjectDto.builder()
                                        .projectId(batch.getProjectId())
                                        .projectName(batch.getProjectName())
                                        .build()
                        )
                        .motorStage(batch.getMotorStage())
                        .numberOfMotors(batch.getNumberOfMotors())
                        .motorIds(batch.getMotorIds())
                        .articles(batch.getArticles())
                        .objective(batch.getObjective())
                        .systemManager(batch.getSystemManager())
                        .updatedBy(batch.getUpdatedBy())
                        .identificationSheet(sheetDto)
                        .currentStage(displayCurrentStages)
                        .stageProgress(displayStageProgress)
                        .identificationSheetStatus(batch.getIdentificationSheetStatus())
                        .createdOn(batch.getCreatedAt())
                        .createdBy(batch.getCreatedBy())
                        .updatedOn(batch.getUpdatedAt())
                        .build())
                .build();
    }

    private List<Batch.StageProgress> filterBemMotorStatusesForDisplay(
            List<Batch.StageProgress> stages,
            Batch batch,
            SubScaleMetadata subScaleMetadata,
            List<QcStageDivisionStatusDto> legacyQcDivisionStatuses) {
        if (stages == null || stages.isEmpty()) return Collections.emptyList();

        Set<String> allowedMotorIds = new LinkedHashSet<>();
        if (batch.getMotorIds() != null) {
            batch.getMotorIds().stream()
                    .filter(Objects::nonNull)
                    .filter(motorId -> !motorId.isBlank())
                    .forEach(allowedMotorIds::add);
        }
        if (subScaleMetadata != null && subScaleMetadata.getBemMotors() != null) {
            subScaleMetadata.getBemMotors().stream()
                    .filter(Objects::nonNull)
                    .map(item -> item.getBemNo())
                    .filter(Objects::nonNull)
                    .filter(motorId -> !motorId.isBlank())
                    .forEach(allowedMotorIds::add);
        }

        return stages.stream().map(stage -> {
            List<MotorStatusResponse> motorStatuses = stage.getMotorStatuses();
            if (Objects.equals(stage.getSubDepartmentId(), STATIC_TESTING_SUB_DEPARTMENT_ID)
                    && motorStatuses != null) {
                motorStatuses = motorStatuses.stream()
                        .filter(Objects::nonNull)
                        .filter(status -> allowedMotorIds.contains(status.getMotorId()))
                        .toList();
            }
            List<QcStageDivisionStatusDto> divisionStatuses = stage.getDivisionStatuses();
            if (Objects.equals(
                    stage.getSubDepartmentId(), QcDivisionCatalog.SUB_DEPARTMENT_ID)
                    && !hasCompleteQcDivisionStatuses(divisionStatuses)) {
                divisionStatuses = legacyQcDivisionStatuses;
            }
            return Batch.StageProgress.builder()
                    .departmentId(stage.getDepartmentId())
                    .departmentName(stage.getDepartmentName())
                    .subDepartmentId(stage.getSubDepartmentId())
                    .subDepartmentName(stage.getSubDepartmentName())
                    .status(stage.getStatus())
                    .enteredAt(stage.getEnteredAt())
                    .updatedAt(stage.getUpdatedAt())
                    .remarks(stage.getRemarks())
                    .rejectionReason(stage.getRejectionReason())
                    .submittedBy(stage.getSubmittedBy())
                    .submittedOn(stage.getSubmittedOn())
                    .approvedBy(stage.getApprovedBy())
                    .approvedOn(stage.getApprovedOn())
                    .premixStatuses(stage.getPremixStatuses())
                    .finalMixStatuses(stage.getFinalMixStatuses())
                    .motorStatuses(motorStatuses)
                    .divisionStatuses(divisionStatuses)
                    .build();
        }).toList();
    }

    private List<QcStageDivisionStatusDto> resolveLegacyQcDivisionStatuses(Batch batch) {
        Batch.StageProgress qcStage = safeStageProgress(batch).stream()
                .filter(stage -> Objects.equals(
                        stage.getSubDepartmentId(), QcDivisionCatalog.SUB_DEPARTMENT_ID))
                .findFirst()
                .orElse(null);
        if (qcStage == null) {
            return List.of();
        }
        if (hasCompleteQcDivisionStatuses(qcStage.getDivisionStatuses())) {
            return qcStage.getDivisionStatuses();
        }
        return qcDivisionStatusResolver.resolveLegacyStatuses(batch);
    }

    private boolean hasCompleteQcDivisionStatuses(
            List<QcStageDivisionStatusDto> divisionStatuses) {
        return divisionStatuses != null
                && !divisionStatuses.isEmpty()
                && divisionStatuses.stream().allMatch(status -> status != null
                && status.getDivisionName() != null
                && status.getDivisionSubmissionType() != null);
    }

    public void enrichMetadataWithRadiography(BatchMetadata metadata) {
        if (metadata == null || metadata.getRocketMotorCasing() == null) {
            return;
        }

        List<RocketMotorMetadata> motors = metadata.getRocketMotorCasing().getMotors();
        if (motors == null || motors.isEmpty()) {
            return;
        }

        for (RocketMotorMetadata motor : motors) {
            if (motor.getMotorId() != null) {

                // 1. Fetch the RocketMotorCasing record by motorId
                RadiographyDetails radiographyDetails = rmcRepository
                        .findByMotorCasingId(motor.getMotorCasingId())
                        .map(RocketMotorCasing::getSections)
                        .map(Sections::getRadiographyDetails)
                        .orElse(null);

                // 2. Attach the radiographyDetails to the RocketMotorMetadata
                motor.setRadiographyDetails(radiographyDetails);
            }
        }
    }

    //    --------------------------GET BATCH LIST----------------------------------------------------------------------------------
    public BatchListResponse getBatchList(BatchListRequest request) {
        Page<Batch> page = batchRepository.findBatchesWithFilters(request);
        boolean isStaticTesting = Objects.equals(
                request.getSubDepartmentId(),
                STATIC_TESTING_SUB_DEPARTMENT_ID);
        Map<String, List<String>> bemMotorIdsByBatch = isStaticTesting
                ? subscaleMetadataResolver.resolveApprovedBemMotorIds(
                page.getContent().stream().map(Batch::getBatchId).toList())
                : Collections.emptyMap();

        List<BatchSummaryDto> summaries = page.getContent().stream()
                .map(batch -> {
                    // Extract lotIds from identificationSheet if present
                    List<String> lotIds = Collections.emptyList();
                    if (batch.getIdentificationSheet() != null
                            && batch.getIdentificationSheet().getMaterials() != null) {
                        lotIds = batch.getIdentificationSheet().getMaterials().stream()
                                .filter(mat -> mat.getLotIds() != null)
                                .flatMap(mat -> mat.getLotIds().stream())
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());
                    }

                    Batch.StageProgress requestedStage = request.getSubDepartmentId() == null
                            ? null
                            : safeStageProgress(batch).stream()
                            .filter(stage -> Objects.equals(
                                    stage.getSubDepartmentId(),
                                    request.getSubDepartmentId()))
                            .findFirst()
                            .orElse(null);
                    List<Batch.StageProgress> currentStages = stageProgressDisplayEnricher.enrichCopies(
                            batch, currentStagesForDisplay(batch));
                    List<Batch.StageProgress> stageProgress = stageProgressDisplayEnricher.enrichCopies(
                            batch, safeStageProgress(batch));
                    Status displayStatus = requestedStage != null
                            ? requestedStage.getStatus()
                            : isAwaitingStageAssignment(batch)
                            ? Status.YET_TO_START
                            : null;

                    return BatchSummaryDto.builder()
                            .id(batch.getBatchId())           // using batchId as id
                            .batchId(batch.getBatchId())
                            .motorIds(batch.getMotorIds())
                            .bemMotorIds(isStaticTesting
                                    ? bemMotorIdsByBatch.getOrDefault(
                                    batch.getBatchId(),
                                    Collections.emptyList())
                                    : null)
                            .motorStage(batch.getMotorStage())
                            .lotIds(lotIds)
                            .project(
                                    ProjectDto.builder()
                                            .projectId(batch.getProjectId())
                                            .projectName(batch.getProjectName())
                                            .build()
                            )
                            .batchType(batch.getBatchType())
                            .parallelFlowEnabled(batch.getParallelFlowEnabled())
                            .isBatchLocked(batch.getLockedForEdit())
                            .systemManager(batch.getSystemManager())
                            .subDepartmentId(requestedStage != null
                                    ? requestedStage.getSubDepartmentId()
                                    : null)
                            .status(displayStatus)
                            .currentStage(currentStages)
                            .stageProgress(stageProgress)
                            .identificationSheetStatus(batch.getIdentificationSheetStatus())
                            .createdOn(batch.getCreatedAt())
                            .createdBy(batch.getCreatedBy())
                            .build();
                })
                .collect(Collectors.toList());

        return BatchListResponse.builder()
                .batches(summaries)
                .pagination(PaginationDto.builder()
                        .page(request.getPagination().getPage())
                        .pageSize(request.getPagination().getLimit())
                        .totalRecords(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }
//    --------------------------DELETE BATCH----------------------------------------------------------------------------------

    public DeleteBatchResponse softDeleteBatch(DeleteBatchRequest request, String userId) {
        // 1. Fetch the batch (even if deleted, to give a proper error)
        Batch batch = batchRepository.findByBatchId(request.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists with batchId: " + request.getBatchId()));

        // 2. Check if already deleted
        if (batch.isDeleted()) {
            throw new AcemException(ErrorCode.INVALID_STATE,
                    "Batch is already deleted");
        }

        boolean canDelete = false;

        // Case 1: Batch has not been assigned to any initial stage yet
        if (batch.getStageProgress() == null || batch.getStageProgress().isEmpty()) {
            canDelete = true;
        }
        // Case 2: Batch is at its dynamic FIRST stage (3L for standard, 8L for SUBSCALE) and not yet started
        else {
            Long firstStageSubDeptId = batchStageSequence.getFirstStage(batch.getBatchType());

            boolean onlyFirstStageUnlocked = batch.getStageProgress().size() == 1
                    && firstStageSubDeptId != null
                    && firstStageSubDeptId.equals(batch.getStageProgress().get(0).getSubDepartmentId());
            if (onlyFirstStageUnlocked) {
                Batch.StageProgress currentStage = batch.getStageProgress().stream()
                        .filter(sp -> firstStageSubDeptId.equals(sp.getSubDepartmentId()))
                        .findFirst()
                        .orElseThrow(() -> new AcemException(
                                ErrorCode.INVALID_STATE,
                                "Current stage not found"));

                canDelete = currentStage.getStatus() == Status.TO_BE_INITIATED;
            }
        }

        if (!canDelete) {
            throw new AcemException(
                    ErrorCode.INVALID_STATE,
                    "Batch cannot be deleted because processing has already started");
        }

        // 4. Fetch the user performing the deletion
        User admin = userRepository.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND,
                        "User not found: " + userId));

        // 5. Soft delete
        batch.setDeleted(true);
        batch.setDeleteReason(request.getReason());
        batch.setDeletedAt(LocalDateTime.now());
        batch.setDeletedBy(PersonInfo.builder()
                .id(userId)
                .fullName(admin.getUsername())
                .build());

        batchRepository.save(batch);

        log.info("Batch {} deleted by {}", request.getBatchId(), userId);

        return DeleteBatchResponse.builder()
                .batchId(batch.getBatchId())
                .deletedOn(batch.getDeletedAt())
                .deletedBy(batch.getDeletedBy())
                .reason(batch.getDeleteReason())
                .build();
    }

    //    --------------------------BATCH STATS----------------------------------------------------------------------------------
    public BatchStatsResponse getBatchStats(BatchStatsRequest request) {

        DateRange range = resolveDateRange(
                request.getFilterType(),
                request.getStartDate(),
                request.getEndDate());

        LocalDateTime start = range.start();
        LocalDateTime end = range.end();

        long totalBatches = batchRepository.countByCreatedAtBetweenAndIsDeletedFalse(start, end);

        List<Batch> batches = batchRepository
                .findByCreatedAtBetweenAndIsDeletedFalse(start, end);


        long inProgress = batches.stream()
                .filter(batch -> batch.getCurrentStage().stream()
                        .anyMatch(stage -> stage.getStatus() == Status.IN_PROGRESS))
                .count();

        long completed = batches.stream()
                .filter(batch -> {
                    return batch.getStageProgress().stream()
                            .anyMatch(stage ->
                                    batchStageSequence.isLastStage(
                                            stage.getSubDepartmentId(),
                                            batch.getBatchType())
                                            && stage.getStatus() == Status.COMPLETELY_APPROVED);
                })
                .count();

        long pending = batches.stream()
                .filter(batch -> batch.getStageProgress().stream()
                        .anyMatch(s -> s.getStatus() == Status.WAITING_FOR_APPROVAL
                                || s.getStatus() == Status.WAITING_FOR_PARTIAL_APPROVAL
                                || s.getStatus() == Status.COMPLETELY_APPROVED))
                .count();

        long rejected = batches.stream()
                .filter(batch -> batch.getStageProgress().stream()
                        .anyMatch(s -> s.getStatus() == Status.REJECTED))
                .count();

        return BatchStatsResponse.builder()
                .totalBatches(
                        StatMetric.builder()
                                .count(totalBatches)
                                .subValue(totalBatches)
                                .build())
                .inProgressBatches(
                        StatMetric.builder()
                                .count(inProgress)
                                .subValue(inProgress)
                                .build())
                .completedBatches(
                        StatMetric.builder()
                                .count(completed)
                                .subValue(completed)
                                .build())
                .pendingApprovals(
                        StatMetric.builder()
                                .count(pending)
                                .subValue(pending)
                                .build())
                .rejectedBatches(
                        StatMetric.builder()
                                .count(rejected)
                                .subValue(rejected)
                                .build())
                .build();
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }

    public void updateRocketMotorMetadata(String batchId, List<String> newOrUpdatedMotorIds) {

        // 1. Fetch batch entity
        Batch batch = batchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "Batch not found for batchId: " + batchId));

        // 2. Ensure IdentificationSheet and Metadata containers exist
        if (batch.getIdentificationSheet() == null) {
            batch.setIdentificationSheet(new BatchIdentificationSheet());
        }

        if (batch.getIdentificationSheet().getMetadata() == null) {
            batch.getIdentificationSheet().setMetadata(new BatchMetadata());
        }

        BatchMetadata metadata = batch.getIdentificationSheet().getMetadata();

        // 3. Collect existing motor IDs from current metadata (if present)
        Set<String> allMotorIds = new LinkedHashSet<>();

        if (metadata.getRocketMotorCasing() != null
                && metadata.getRocketMotorCasing().getMotors() != null) {

            metadata.getRocketMotorCasing().getMotors().stream()
                    .map(RocketMotorMetadata::getMotorId)
                    .filter(Objects::nonNull)
                    .forEach(allMotorIds::add);
        }

        // 4. Merge incoming motor IDs (preserves order and eliminates duplicates)
        if (newOrUpdatedMotorIds != null) {
            allMotorIds.addAll(newOrUpdatedMotorIds);
        }

        // 5. Fetch active casing records for ALL accumulated motor IDs
        List<RocketMotorCasing> casingRecords = !allMotorIds.isEmpty()
                ? rmcRepository.findByMotorIdInAndActiveTrue(new ArrayList<>(allMotorIds))
                : Collections.emptyList();

        // 6. Map casing records to RocketMotorMetadata list
        List<RocketMotorMetadata> motorItems = casingRecords.stream().map(casing -> {

            String casingType = Optional.ofNullable(casing.getSections())
                    .map(Sections::getMotorReceipt)
                    .map(MotorReceipt::getCasingType)
                    .orElse(null);

            String insulationType = Optional.ofNullable(casing.getSections())
                    .map(Sections::getMotorReceipt)
                    .map(MotorReceipt::getInsulation)
                    .map(Insulation::getType)
                    .map(Object::toString)
                    .orElse(null);

            return RocketMotorMetadata.builder()
                    .motorCasingId(casing.getMotorCasingId())
                    .motorId(casing.getMotorId())
                    .castingType(casingType)
                    .insulationType(insulationType)
                    .build();
        }).toList();

        // 7. Construct RocketMotorListdata
        RocketMotorListdata rocketMotorListdata = RocketMotorListdata.builder()
                .motors(motorItems)
                .build();

        // 8. Update rocketMotorCasing while preserving existing RMP metadata
        metadata.setRocketMotorCasing(rocketMotorListdata);

        // 9. Persist updated batch
        batchRepository.save(batch);
    }

    private DateRange resolveDateRange(String filterType,
                                       String startDate, String endDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = switch (filterType) {
            case "day" -> now.toLocalDate().atStartOfDay();
            case "week" -> now.toLocalDate()
                    .with(DayOfWeek.MONDAY).atStartOfDay();
            case "month" -> now.toLocalDate()
                    .withDayOfMonth(1).atStartOfDay();
            case "custom" -> {
                if (startDate == null || startDate.isBlank())
                    throw new AcemException(ErrorCode.INVALID_REQUEST,
                            "startDate is required for custom filter");
                yield DateTimeConversionUtil.parseLocalDate(startDate).atStartOfDay();
            }
            default -> throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "filterType must be day, week, month, or custom");
        };

        LocalDateTime end = now;
        if (endDate != null && !endDate.isBlank()) {
            end = DateTimeConversionUtil.parseLocalDate(endDate).atTime(23, 59, 59);
        }

        if (start.isAfter(end)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "startDate cannot be greater than endDate");
        }

        return new DateRange(start, end);
    }

    // ─────── validation ───────
    private void validateRequest(BatchRequest request) {
        if (request.getBatchType() == null || request.getBatchType().isBlank()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "batchType is required");
        }
        String type = request.getBatchType().toUpperCase();
        if ("MAIN".equals(type)) {
            if (request.getProjectId() == null || request.getProjectId().isBlank())
                throw new AcemException(ErrorCode.INVALID_REQUEST, "projectId is required for MAIN batch");
            if (request.getMotorIds() == null || request.getMotorIds().isEmpty())
                throw new AcemException(ErrorCode.INVALID_REQUEST, "motorIds are required");
//            validateCompositionSum(request.getIdentificationSheet());
        } else if ("SUBSCALE".equals(type)) {
            if (request.getSubBatchType() == null)
                throw new AcemException(ErrorCode.INVALID_REQUEST, "subBatchType is required for SUBSCALE");
            String sub = request.getSubBatchType().toUpperCase();
            if ("QUALIFICATION".equals(sub)) {
                if (request.getProjectId() == null)
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "projectId is required for QUALIFICATION");
                if (request.getMotorIds() == null || request.getMotorIds().isEmpty())
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "motorIds are required");
            } else if ("EXPERIMENTAL".equals(sub)) {
                if (request.getArticles() == null || request.getArticles().isEmpty())
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "articles are required");
                if (request.getObjective() == null || request.getObjective().isBlank())
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "objective is required for EXPERIMENTAL");
            } else {
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Invalid subBatchType");
            }
        }
        boolean isSubscale = "SUBSCALE".equals(type);
        if (!isSubscale && (request.getSystemManagerId() == null || request.getSystemManagerId().isBlank())) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "systemManagerId is required");
        }
        if (request.getPriority() == null || request.getPriority().isBlank())
            throw new AcemException(ErrorCode.INVALID_REQUEST, "priority is required");
    }

    //    private void validateCompositionSum(IdentificationSheetDto sheet) {
//        if (sheet != null && sheet.getMaterials() != null) {
//            double sum = sheet.getMaterials().stream()
//                    .filter(m -> m.getRequiredComposition() != null)
//                    .mapToDouble(BatchMaterialDto::getRequiredComposition).sum();
//            if (Math.abs(sum - 100.0) > 0.01) {
//                throw new AcemException(ErrorCode.BUSINESS_VALIDATION_FAILED,
//                        "Total required composition must equal 100%");
//            }
//        }
//    }
    private void validateMotorIds(BatchRequest request) {
        if (request.getMotorIds() == null || request.getMotorIds().isEmpty()) {
            return;   // already handled by other checks
        }

        // Fetch approved casings for this project and stage
        List<RocketMotorCasing> approved = rmcRepository.findByProjectIdAndMotorStageAndStatusAndActiveTrue(
                request.getProjectId(),
                request.getMotorStage(),
                Status.APPROVED
        );

        Set<String> approvedMotorIds = approved.stream()
                .map(RocketMotorCasing::getMotorId)
                .collect(Collectors.toSet());

        List<String> invalid = request.getMotorIds().stream()
                .filter(id -> !approvedMotorIds.contains(id))
                .collect(Collectors.toList());

        if (!invalid.isEmpty()) {
            throw new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                    "The following motor IDs are not approved or do not exist: "
                            + String.join(", ", invalid));
        }

        rejectUnavailableMotors(request.getMotorIds(), Set.of());
    }

    private void validateReferences(BatchRequest request) {
        if (request.getSystemManagerId() != null && !request.getSystemManagerId().isBlank()) {
            userRepository.findByUserId(request.getSystemManagerId())
                    .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                            "System Manager not found: " + request.getSystemManagerId()));
        }

        if (request.getProjectId() != null && !request.getProjectId().isBlank()) {
            projectRepository.findByProjectId(request.getProjectId())
                    .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                            "Project not found: " + request.getProjectId()));
        }

        if (request.getIdentificationSheet() != null
                && request.getIdentificationSheet().getMaterials() != null) {
            for (BatchMaterialDto mat : request.getIdentificationSheet().getMaterials()) {
                if (mat.getLotIds() != null) {
                    for (String lotId : mat.getLotIds()) {
                        Lot lot = lotRepository.findByLotId(lotId)
                                .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                                        "Lot not found: " + lotId));
                        if (lot.getStatus() == Status.DELETED)
                            throw new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                                    "Lot is deleted: " + lotId);
                        if (!lot.getMaterialCode().equals(mat.getMaterialCode()))
                            throw new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                                    "Material code mismatch for lot " + lotId);
                    }
                }
            }
        }
    }

    private String computeIdentificationSheetStatus(IdentificationSheetDto sheet) {

        if (sheet == null) return "DRAFT";

        if (sheet.getDate() == null
                || sheet.getBatchSize() == null
                || isBlank(sheet.getBondingSheetNo())
                || isBlank(resolveMixerTypeCode(sheet.getMixerType()))
                || sheet.getNumberOfPremix() == null
                || isBlank(sheet.getRemarks())) {
            return "DRAFT";
        }

        if (sheet.getMaterials() == null || sheet.getMaterials().isEmpty()) {
            return "DRAFT";
        }

        for (BatchMaterialDto m : sheet.getMaterials()) {
            if (m.getSrNo() == null
                    || isBlank(m.getMaterialCode())
                    || m.getLotIds() == null
                    || m.getLotIds().isEmpty()
                    || m.getLotIds().stream().anyMatch(String::isBlank)
                    || isBlank(m.getMake())
                    || m.getRequiredComposition() == null
                    || m.getQuantityPerPremix() == null) {
                return "DRAFT";
            }
        }

        return "COMPLETED";
    }

    /**
     * True when sheet would be COMPLETED if mixerType were present (legacy round-trip).
     */
    private boolean sheetCompleteExceptMixer(IdentificationSheetDto sheet) {
        if (sheet == null) return false;
        if (sheet.getDate() == null
                || sheet.getBatchSize() == null
                || isBlank(sheet.getBondingSheetNo())
                || sheet.getNumberOfPremix() == null
                || isBlank(sheet.getRemarks())) {
            return false;
        }
        if (sheet.getMaterials() == null || sheet.getMaterials().isEmpty()) {
            return false;
        }
        for (BatchMaterialDto m : sheet.getMaterials()) {
            if (m.getSrNo() == null
                    || isBlank(m.getMaterialCode())
                    || m.getLotIds() == null
                    || m.getLotIds().isEmpty()
                    || m.getLotIds().stream().anyMatch(String::isBlank)
                    || isBlank(m.getMake())
                    || m.getRequiredComposition() == null
                    || m.getQuantityPerPremix() == null) {
                return false;
            }
        }
        return true;
    }

    private void setYetToAssignStage(Batch batch, LocalDateTime timestamp) {
        batch.setCurrentStage(new ArrayList<>(List.of(
                Batch.StageProgress.builder()
                        .departmentName("Yet to Assign")
                        .subDepartmentName("Yet to Assign")
                        .status(Status.YET_TO_START)
                        .enteredAt(timestamp)
                        .updatedAt(timestamp)
                        .build())));
    }

    private List<Batch.StageProgress> currentStagesForDisplay(Batch batch) {
        if (batch.getCurrentStage() != null && !batch.getCurrentStage().isEmpty()) {
            return batch.getCurrentStage();
        }
        if (!isAwaitingStageAssignment(batch)) {
            return Collections.emptyList();
        }
        LocalDateTime timestamp = batch.getUpdatedAt() != null
                ? batch.getUpdatedAt()
                : batch.getCreatedAt();
        return List.of(Batch.StageProgress.builder()
                .departmentName("Yet to Assign")
                .subDepartmentName("Yet to Assign")
                .status(Status.YET_TO_START)
                .enteredAt(timestamp)
                .updatedAt(timestamp)
                .build());
    }

    private List<Batch.StageProgress> safeStageProgress(Batch batch) {
        return batch.getStageProgress() != null
                ? batch.getStageProgress()
                : Collections.emptyList();
    }

    private boolean isAwaitingStageAssignment(Batch batch) {
        return "DRAFT".equalsIgnoreCase(batch.getIdentificationSheetStatus())
                && safeStageProgress(batch).isEmpty();
    }

    private void validateUpdateRequest(BatchRequest request) {
        if (request.getBatchType() == null || request.getBatchType().isBlank()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "batchType is required");
        }

        String type = request.getBatchType().toUpperCase();
        boolean isSubscale = "SUBSCALE".equals(type);

        if ("MAIN".equals(type)) {
            if (request.getProjectId() == null || request.getProjectId().isBlank())
                throw new AcemException(ErrorCode.INVALID_REQUEST, "projectId is required for MAIN");
            if (request.getMotorIds() == null || request.getMotorIds().isEmpty())
                throw new AcemException(ErrorCode.INVALID_REQUEST, "motorIds are required");
            // motorIds is source of truth; align count (FE draft field is often 0 after add)
            request.setNumberOfMotors(request.getMotorIds().size());
        } else if (isSubscale) {
            if (request.getSubBatchType() == null)
                throw new AcemException(ErrorCode.INVALID_REQUEST, "subBatchType is required");
            String sub = request.getSubBatchType().toUpperCase();
            if ("QUALIFICATION".equals(sub)) {
                if (request.getProjectId() == null)
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "projectId is required");
                if (request.getMotorIds() == null || request.getMotorIds().isEmpty())
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "motorIds are required");
                request.setNumberOfMotors(request.getMotorIds().size());
            } else if ("EXPERIMENTAL".equals(sub)) {
                if (request.getArticles() == null || request.getArticles().isEmpty())
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "articles are required");
                if (request.getObjective() == null || request.getObjective().isBlank())
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "objective is required");
            }
        }

        if (!isSubscale) {
            if (request.getSystemManagerId() == null || request.getSystemManagerId().isBlank()) {
                throw new AcemException(ErrorCode.INVALID_REQUEST, "systemManagerId is required for main batches");
            }
        }
    }

    private void validateMotorIdsForUpdate(BatchRequest request) {
        if (request.getMotorIds() == null || request.getMotorIds().isEmpty()) {
            return;   // already handled by other checks
        }

        List<RocketMotorCasing> approved = rmcRepository.findByProjectIdAndMotorStageAndStatusAndActiveTrue(
                request.getProjectId(),
                request.getMotorStage(),
                Status.APPROVED
        );

        Set<String> approvedMotorIds = approved.stream()
                .map(RocketMotorCasing::getMotorId)
                .collect(Collectors.toSet());

        List<String> invalid = request.getMotorIds().stream()
                .filter(id -> !approvedMotorIds.contains(id))
                .collect(Collectors.toList());

        if (!invalid.isEmpty()) {
            throw new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                    "The following motor IDs are not approved or do not exist: "
                            + String.join(", ", invalid));
        }

        Set<String> alreadyOnBatch = Set.of();
        if (request.getBatchId() != null && !request.getBatchId().isBlank()) {
            alreadyOnBatch = batchRepository.findByBatchId(request.getBatchId())
                    .map(Batch::getMotorIds)
                    .filter(Objects::nonNull)
                    .map(ids -> ids.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(id -> !id.isEmpty())
                            .collect(Collectors.toSet()))
                    .orElse(Set.of());
        }
        rejectUnavailableMotors(request.getMotorIds(), alreadyOnBatch);
    }


    private void rejectUnavailableMotors(List<String> motorIds, Set<String> allowlist) {
        if (motorIds == null || motorIds.isEmpty()) {
            return;
        }
        Set<String> candidates = motorIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
        if (candidates.isEmpty()) {
            return;
        }

        Query query = new Query(new Criteria().andOperator(
                Criteria.where("isDeleted").ne(true),
                Criteria.where("motorIds").in(candidates)));
        List<Batch> batches = mongoTemplate.find(query, Batch.class);
        Set<String> unavailable = MotorBatchLifecycleHelper.collectUnavailableMotorIds(batches);

        List<String> blocked = motorIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .filter(id -> MotorBatchLifecycleHelper.containsIgnoreCase(unavailable, id))
                .filter(id -> !MotorBatchLifecycleHelper.containsIgnoreCase(allowlist, id))
                .distinct()
                .collect(Collectors.toList());

        if (!blocked.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "The following motor IDs are discarded or already dispatched and cannot be linked: "
                            + String.join(", ", blocked));
        }
    }

    private void validateReferencesForUpdate(BatchRequest request) {
        boolean isSubscale = "SUBSCALE".equalsIgnoreCase(request.getBatchType());
        boolean hasSystemManager = request.getSystemManagerId() != null
                && !request.getSystemManagerId().isBlank();

        if (!isSubscale) {
            if (!hasSystemManager) {
                throw new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                        "System Manager not found: " + request.getSystemManagerId());
            }
            userRepository.findByUserId(request.getSystemManagerId())
                    .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                            "System Manager not found: " + request.getSystemManagerId()));
        } else if (hasSystemManager) {
            // Optional for subscale, but validate if provided
            userRepository.findByUserId(request.getSystemManagerId())
                    .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                            "System Manager not found: " + request.getSystemManagerId()));
        }

        if (request.getProjectId() != null) {
            projectRepository.findByProjectId(request.getProjectId())
                    .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                            "Project not found: " + request.getProjectId()));
        }

        if (request.getIdentificationSheet() != null
                && request.getIdentificationSheet().getMaterials() != null) {
            for (BatchMaterialDto mat : request.getIdentificationSheet().getMaterials()) {
                if (mat.getLotIds() != null) {
                    for (String lotId : mat.getLotIds()) {
                        Lot lot = lotRepository.findByLotId(lotId)
                                .orElseThrow(() -> new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                                        "Lot not found: " + lotId));
                        if (lot.getStatus() == Status.DELETED)
                            throw new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                                    "Lot is deleted: " + lotId);
                        if (!lot.getMaterialCode().equals(mat.getMaterialCode()))
                            throw new AcemException(ErrorCode.DEPENDENCY_NOT_FOUND,
                                    "Material code mismatch for lot " + lotId);
                    }
                }
            }
        }
    }

    private BatchIdentificationSheet buildIdentificationSheet(IdentificationSheetDto req) {

        if (req == null) {
            return null;
        }

        // Fetch Mixer
        MasterDataDto mixer = null;
        String mixerCode = resolveMixerTypeCode(req.getMixerType());
        if (mixerCode != null) {
            MixerMaster mixerMaster = mixerMasterRepository.findByCode(mixerCode)
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Mixer not found: " + mixerCode
                    ));
            if (mixerMaster.getIsActive() != null && !Boolean.TRUE.equals(mixerMaster.getIsActive())) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Mixer is inactive: " + mixerCode
                );
            }

            mixer = MasterDataDto.builder()
                    .id(mixerMaster.getId())
                    .code(mixerMaster.getCode())
                    .name(mixerMaster.getName())
                    .build();
        }

        List<BatchMaterial> materials = req.getMaterials() == null
                ? List.of()
                : java.util.stream.IntStream.range(0, req.getMaterials().size())
                .mapToObj(index -> {
                    BatchMaterialDto m = req.getMaterials().get(index);

                    MaterialGrade grade = null;

                    MaterialMaster material = materialRepository
                            .findFirstByMaterialCode(m.getMaterialCode())
                            .orElseThrow(() -> new AcemException(
                                    ErrorCode.RESOURCE_NOT_FOUND,
                                    "Material not found: " + m.getMaterialCode()
                            ));
                    if (!MaterialMasterDataService.isActive(material)) {
                        throw new AcemException(
                                ErrorCode.INVALID_REQUEST,
                                "Material is inactive: " + m.getMaterialCode()
                        );
                    }

                    if (m.getGradeCode() != null) {
                        grade = material.getGrades().stream()
                                .filter(g ->
                                        m.getGradeCode().equalsIgnoreCase(g.getGradeCode())
                                                || m.getGradeCode().equalsIgnoreCase(g.getGradeName()))
                                .findFirst()
                                .orElseThrow(() -> new AcemException(
                                        ErrorCode.RESOURCE_NOT_FOUND,
                                        "Invalid grade '" + m.getGradeCode()
                                                + "' for material " + m.getMaterialCode()
                                ));
                    }

                    // Always assign sequential srNo (1..n) so client gaps/duplicates after delete never persist.
                    return BatchMaterial.builder()
                            .srNo(index + 1)
                            .materialCode(m.getMaterialCode())
                            .grade(grade)
                            .lotIds(m.getLotIds())
                            .make(m.getMake())
                            .requiredComposition(m.getRequiredComposition())
                            .quantityPerPremix(m.getQuantityPerPremix())
                            .revalidationFromDate(m.getRevalidationFromDate())
                            .revalidationToDate(m.getRevalidationToDate())
                            .build();
                })
                .toList();

        return BatchIdentificationSheet.builder()
                .date(req.getDate())
                .batchSize(req.getBatchSize())
                .bondingSheetNo(req.getBondingSheetNo())
                .mixerType(mixer)
                .numberOfPremix(req.getNumberOfPremix())
                .remarks(req.getRemarks())
                .prcApprovalDate(req.getPrcApprovalDate())
                .materials(materials)
                .build();
    }

    public SubDeptStatsResponse getApproverSubDeptStats(SubDeptStatsRequest request) {

        Long subDeptId = request.getSubDepartmentId();

        departmentRepo.findBySubDepartmentsSubDepartmentId(subDeptId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.SUB_DEPARTMENTS_NOT_FOUND,
                        "SubDepartment not found: " + subDeptId));

        if (subDeptId == 1L) {
            allocated = lotRepository.countBySubDepartmentIdAndStatusNot(subDeptId, Status.DELETED);
            pending = lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.WAITING_FOR_APPROVAL);
            approved = lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.APPROVED);
            rejected = lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.REJECTED);
        } else if (subDeptId == 2L) {
            allocated = rmcRepository.countBySubDepartmentIdAndActiveTrue(subDeptId);
            pending = rmcRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.WAITING_FOR_APPROVAL);
            approved = rmcRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.APPROVED);
            rejected = rmcRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.REJECTED);
        } else {
            allocated = batchRepository.countByStageProgressSubDepartmentIdAndIsDeletedFalse(subDeptId);
            approved = batchRepository.countByStageProgressSubDepartmentIdAndStatus(
                    subDeptId, Status.COMPLETELY_APPROVED);
            rejected = batchRepository.countByStageProgressSubDepartmentIdAndStatus(subDeptId, Status.REJECTED);
            pending = batchRepository.countByStageProgressSubDepartmentIdAndStatusIn(
                    subDeptId,
                    List.of(
                            Status.WAITING_FOR_APPROVAL,
                            Status.WAITING_FOR_PARTIAL_APPROVAL));
        }
        return SubDeptStatsResponse.builder()
                .allocated(allocated)
                .approved(approved)
                .rejected(rejected)
                .pending(pending)
                .build();

    }

    public SubDeptStatsResponse getUserSubDeptStats(SubDeptStatsRequest request) {

        Long subDeptId = request.getSubDepartmentId();

        departmentRepo.findBySubDepartmentsSubDepartmentId(subDeptId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.SUB_DEPARTMENTS_NOT_FOUND,
                        "SubDepartment not found: " + subDeptId));

        if (subDeptId == 1L) {
            allocated = lotRepository.countBySubDepartmentIdAndStatusNot(subDeptId, Status.DELETED);
            pending = lotRepository.countBySubDepartmentIdAndStatusIn(subDeptId,
                    List.of(Status.TO_BE_INITIATED, Status.IN_PROGRESS));
            approved = lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.APPROVED);
            rejected = lotRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.REJECTED);
        } else if (subDeptId == 2L) {
            allocated = rmcRepository.countBySubDepartmentIdAndActiveTrue(subDeptId);
            pending = rmcRepository.countBySubDepartmentIdAndStatusIn(subDeptId,
                    List.of(Status.TO_BE_INITIATED, Status.IN_PROGRESS));
            approved = rmcRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.APPROVED);
            rejected = rmcRepository.countBySubDepartmentIdAndStatus(subDeptId, Status.REJECTED);
        } else {
            allocated = batchRepository.countByStageProgressSubDepartmentIdAndIsDeletedFalse(subDeptId);
            approved = batchRepository.countByStageProgressSubDepartmentIdAndStatus(
                    subDeptId, Status.COMPLETELY_APPROVED);
            rejected = batchRepository.countByStageProgressSubDepartmentIdAndStatus(subDeptId, Status.REJECTED);
            pending = batchRepository.countByStageProgressSubDepartmentIdAndStatusIn(subDeptId,
                    List.of(
                            Status.TO_BE_INITIATED,
                            Status.IN_PROGRESS,
                            Status.WAITING_FOR_PARTIAL_APPROVAL));
        }
        return SubDeptStatsResponse.builder()
                .allocated(allocated)
                .approved(approved)
                .rejected(rejected)
                .pending(pending)
                .build();

    }

    /**
     * Allocates the next batch ID as {@code ACEM/YYMM/NNNN} using the highest
     * existing numeric suffix (including soft-deleted docs) plus an atomic
     * counter. Unlike {@code count()+1}, hard-deleting lower IDs cannot
     * collide with remaining higher sequences.
     */
    private String generateBatchId() {
        long highestExistingSequence = findHighestExistingBatchSequence();

        Query counterQuery = Query.query(
                Criteria.where("_id").is(BATCH_ID_COUNTER_ID)
        );

        try {
            mongoTemplate.upsert(
                    counterQuery,
                    new Update().max("sequence", highestExistingSequence),
                    FormIdCounter.class
            );
        } catch (DuplicateKeyException ignored) {
            // Concurrent bootstrap of the same counter; $inc below still allocates uniquely.
        }

        FormIdCounter counter = mongoTemplate.findAndModify(
                counterQuery,
                new Update().inc("sequence", 1),
                FindAndModifyOptions.options()
                        .returnNew(true)
                        .upsert(true),
                FormIdCounter.class
        );

        if (counter == null) {
            throw new IllegalStateException(
                    "Unable to generate batch ID for " + BATCH_ID_PREFIX
            );
        }

        LocalDate today = LocalDate.now();
        String year = String.format("%02d", today.getYear() % 100);
        String month = String.format("%02d", today.getMonthValue());

        return String.format(
                "ACEM/%s%s/%04d",
                year,
                month,
                counter.getSequence()
        );
    }

    private long findHighestExistingBatchSequence() {
        String regex = BATCH_ID_PATTERN.pattern();

        Document splitId = new Document(
                "$split",
                List.of("$" + BATCH_ID_FIELD, "/")
        );
        Document suffix = new Document(
                "$arrayElemAt",
                List.of(splitId, -1)
        );

        List<Document> pipeline = List.of(
                new Document(
                        "$match",
                        new Document(
                                BATCH_ID_FIELD,
                                new Document("$regex", regex)
                        )
                ),
                new Document(
                        "$project",
                        new Document(
                                "sequence",
                                new Document("$toLong", suffix)
                        )
                ),
                new Document(
                        "$sort",
                        new Document("sequence", -1)
                ),
                new Document("$limit", 1)
        );

        AggregateIterable<Document> results =
                mongoTemplate
                        .getCollection(BATCH_COLLECTION)
                        .aggregate(pipeline);

        Document highest = results.first();
        if (highest == null) {
            return 0;
        }

        Number sequence = highest.get("sequence", Number.class);
        return sequence != null ? sequence.longValue() : 0;
    }

    private static String resolveMixerTypeCode(MasterDataDto mixerType) {
        if (mixerType == null) {
            return null;
        }
        String code = mixerType.getCode();
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public void syncStageProgress(
            String batchId,
            Long subDepartmentId,
            Status status,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            List<MotorStatusResponse> motorStatuses,
            PersonInfo submittedBy,
            LocalDateTime submittedOn,
            boolean approvedUnitEvent,
            String remarks,
            String rejectionReason) {

        stageOrchestrationService.synchronizeStage(
                batchId,
                subDepartmentId,
                BatchStageOrchestrationService.StageUpdate.builder()
                        .status(status)
                        .premixStatuses(premixStatuses)
                        .finalMixStatuses(finalMixStatuses)
                        .motorStatuses(motorStatuses)
                        .submittedBy(submittedBy)
                        .submittedOn(submittedOn)
                        .approvedUnitEvent(approvedUnitEvent)
                        .remarks(remarks)
                        .rejectionReason(rejectionReason)
                        .build());
    }

    public void syncQcStageProgress(
            String batchId,
            Long subDepartmentId,
            Status status,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            List<MotorStatusResponse> motorStatuses,
            List<QcStageDivisionStatusDto> divisionStatuses,
            PersonInfo submittedBy,
            LocalDateTime submittedOn,
            boolean approvedUnitEvent,
            String remarks,
            String rejectionReason) {

        stageOrchestrationService.synchronizeStage(
                batchId,
                subDepartmentId,
                BatchStageOrchestrationService.StageUpdate.builder()
                        .status(status)
                        .premixStatuses(premixStatuses)
                        .finalMixStatuses(finalMixStatuses)
                        .motorStatuses(motorStatuses)
                        .divisionStatuses(divisionStatuses)
                        .submittedBy(submittedBy)
                        .submittedOn(submittedOn)
                        .approvedUnitEvent(approvedUnitEvent)
                        .remarks(remarks)
                        .rejectionReason(rejectionReason)
                        .build());
    }

    @Transactional
    public void syncStageApproval(
            String batchId,
            Long subDepartmentId,
            Status status,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            List<MotorStatusResponse> motorStatuses,
            boolean approvedUnitEvent,
            String remarks,
            String rejectionReason,
            String approverUserId,
            LocalDateTime approvedOn) {

        User approver = userRepository.findByUserId(approverUserId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"));
        PersonInfo approverInfo = PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build();

        stageOrchestrationService.synchronizeStage(
                batchId,
                subDepartmentId,
                BatchStageOrchestrationService.StageUpdate.builder()
                        .status(status)
                        .premixStatuses(premixStatuses)
                        .finalMixStatuses(finalMixStatuses)
                        .motorStatuses(motorStatuses)
                        .approvedBy(approverInfo)
                        .approvedOn(approvedOn)
                        .approvedUnitEvent(approvedUnitEvent)
                        .remarks(remarks)
                        .rejectionReason(rejectionReason)
                        .build());
    }

    public void syncQcStageApproval(
            String batchId,
            Long subDepartmentId,
            Status status,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            List<MotorStatusResponse> motorStatuses,
            List<QcStageDivisionStatusDto> divisionStatuses,
            boolean approvedUnitEvent,
            String remarks,
            String rejectionReason,
            String approverUserId,
            LocalDateTime approvedOn) {

        User approver = userRepository.findByUserId(approverUserId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"));
        PersonInfo approverInfo = PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build();

        stageOrchestrationService.synchronizeStage(
                batchId,
                subDepartmentId,
                BatchStageOrchestrationService.StageUpdate.builder()
                        .status(status)
                        .premixStatuses(premixStatuses)
                        .finalMixStatuses(finalMixStatuses)
                        .motorStatuses(motorStatuses)
                        .divisionStatuses(divisionStatuses)
                        .approvedBy(approverInfo)
                        .approvedOn(approvedOn)
                        .approvedUnitEvent(approvedUnitEvent)
                        .remarks(remarks)
                        .rejectionReason(rejectionReason)
                        .build());
    }

    public void syncAutomaticStageCompletion(
            String batchId,
            Long subDepartmentId,
            List<PremixStatusResponse> premixStatuses,
            List<PremixStatusResponse> finalMixStatuses,
            List<MotorStatusResponse> motorStatuses,
            PersonInfo actor,
            LocalDateTime completedOn,
            String remarks) {
        stageOrchestrationService.synchronizeStage(
                batchId,
                subDepartmentId,
                BatchStageOrchestrationService.StageUpdate.builder()
                        .status(Status.COMPLETELY_APPROVED)
                        .premixStatuses(premixStatuses)
                        .finalMixStatuses(finalMixStatuses)
                        .motorStatuses(motorStatuses)
                        .submittedBy(actor)
                        .submittedOn(completedOn)
                        .approvedBy(actor)
                        .approvedOn(completedOn)
                        .remarks(remarks)
                        .rejectionReason(null)
                        .build());
    }

    public void assertStageUnlockedAndActive(
            Batch batch,
            Long subDepartmentId,
            String stageName) {
        stageOrchestrationService.assertStageUnlockedAndActive(
                batch,
                subDepartmentId,
                stageName);
    }

    public void updateBatchStatus(String batchId, Long currentSubDeptId, Status status, String remarks,
                                  String rejectionReason, String approverUserId) {
        User approver = userRepository.findByUserId(approverUserId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.USER_NOT_FOUND,
                        "User not found"));
        PersonInfo approverInfo = PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build();
        stageOrchestrationService.synchronizeStage(
                batchId,
                currentSubDeptId,
                BatchStageOrchestrationService.StageUpdate.builder()
                        .status(status)
                        .remarks(remarks)
                        .rejectionReason(rejectionReason)
                        .approvedBy(approverInfo)
                        .approvedOn(LocalDateTime.now())
                        .build());
    }


    public void moveToNextStageIfApproved(String batchId, Long currentSubDeptId, String updatedBy) {
        updateBatchStatus(
                batchId,
                currentSubDeptId,
                Status.COMPLETELY_APPROVED,
                null,
                null,
                updatedBy);
    }
}
