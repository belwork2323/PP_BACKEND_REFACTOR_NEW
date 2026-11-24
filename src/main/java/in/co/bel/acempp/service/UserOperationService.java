package in.co.bel.acempp.service;

import in.co.bel.acempp.dto.general.*;
import in.co.bel.acempp.dto.rocketmotorcasing.CasingResponse;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.*;
import in.co.bel.acempp.dto.rocketmotorcasing.MotorStageDto;
import in.co.bel.acempp.dto.rocketmotorcasing.RocketMotorCasingListRequest;
import in.co.bel.acempp.dto.rocketmotorcasing.RocketMotorCasingListResponse;
import in.co.bel.acempp.dto.user.operations.*;
import in.co.bel.acempp.entity.DimensionalParameter;
import in.co.bel.acempp.entity.MotorStage;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.batch.BatchMaterial;
import in.co.bel.acempp.entity.InsulationSpecification;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.entity.sourcing.RocketMotorCasing;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.MaterialType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.*;
import in.co.bel.acempp.repository.sourcing.RocketMotorCasingRepository;
import in.co.bel.acempp.service.batch.StageProgressDisplayEnricher;
import in.co.bel.acempp.service.masterdata.DimensionalParameterMasterDataHandler;
import in.co.bel.acempp.service.masterdata.InsulationSpecificationMasterDataService;
import in.co.bel.acempp.service.masterdata.MaterialMasterDataService;
import in.co.bel.acempp.service.masterdata.MotorStageMasterDataHandler;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserOperationService {

    private static final long STATIC_TESTING_SUB_DEPARTMENT_ID = 12L;

    private final MaterialRepository materialRepo;
    private final DepartmentRepository departmentRepo;
    private final BatchRepository batchRepository;
    private final DimensionalParameterRepository dimParamRepo;
    private final MotorStageRepository motorStageRepository;
    private final MongoTemplate mongoTemplate;
    private final CastingStationRepository castingStationRepository;
    private final BatchFormResolver batchFormResolver;
    private final SubscaleMetadataResolver subscaleMetadataResolver;
    private final RocketMotorCasingRepository rmcRepository;
    private final InsulationSpecificationRepository insRepository;
    private final ProjectRepository projectRepo;
    private final StageProgressDisplayEnricher stageProgressDisplayEnricher;
    private static final Set<String> WAITING_APPROVAL_GROUP = Set.of(
            "WAITING_FOR_PARTIAL_APPROVAL",
            "WAITING_FOR_APPROVAL"
    );

    // ─────────────────────────── MATERIALS LIST ───────────────────────────
    public List<MaterialDto> getMaterialsList(MaterialListRequest request) {

        if (request.getMaterialType() == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid request",
                    "materialType is required");
        }

        List<MaterialMaster> materials;

        if ("BOTH".equalsIgnoreCase(request.getMaterialType())) {
            materials = materialRepo.findAll();
        } else {
            MaterialType materialType;
            try {
                materialType = MaterialType.valueOf(
                        request.getMaterialType().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Invalid request",
                        "materialType must be SOLID, LIQUID or BOTH");
            }

            materials = materialRepo.findByMaterialType(materialType);
        }

        materials = materials.stream()
                .filter(MaterialMasterDataService::isActive)
                .filter(material -> matchesRawMaterialCategory(material, request.getRawMaterialType()))
                .collect(Collectors.toList());

        if (materials.isEmpty()) {
            throw new AcemException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "No materials found",
                    "No materials available in the system");
        }

        return materials.stream()
                .map(material -> MaterialDto.builder()
                        .materialId(material.getMaterialId())
                        .materialCode(material.getMaterialCode())
                        .materialName(material.getMaterialName())
                        .rawMaterialType(MaterialMasterDataService
                                .resolveRawMaterialCategory(material.getRawMaterialType()).name())
                        .preparationType(material.getPreparationType() == null
                                ? null
                                : material.getPreparationType().toValue())
                        .rmpFormTemplate(MaterialMasterDataService
                                .resolveRmpFormTemplate(material).toValue())
                        .grades(material.getGrades() == null
                                ? Collections.emptyList()
                                : material.getGrades().stream()
                                .map(grade -> MaterialGrade.builder()
                                        .gradeId(grade.getGradeId())
                                        .gradeCode(grade.getGradeCode())
                                        .gradeName(grade.getGradeName())
                                        .build())
                                .collect(Collectors.toList()))
                        .specCount(material.getSpecifications() == null
                                        ? 0L
                                        : (long) material.getSpecifications().size())
                        .build())
                .collect(Collectors.toList());
    }
    // ───────────────────────── USER DASHBOARD BATCH LIST ────────────────────────

    public UserBatchListResponse getBatchList(UserBatchListRequest request) {
        Long subDeptId = request.getSubDepartmentId();

        if (request.getFromDate() != null && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            throw new AcemException(
                    ErrorCode.INVALID_DATE_RANGE,
                    "Invalid date range",
                    "fromDate cannot be after toDate");
        }

        // Validate sub‑department exists
        departmentRepo.findBySubDepartmentsSubDepartmentId(subDeptId)
                .orElseThrow(() -> new AcemException(ErrorCode.SUB_DEPARTMENTS_NOT_FOUND,
                        "SubDepartment not found: " + subDeptId));

        List<String> statuses = null;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            List<String> expanded = request.getStatus().stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().replace(" ", "_").toUpperCase(Locale.ROOT))
                    .filter(s -> !s.isBlank())
                    .flatMap(status -> WAITING_APPROVAL_GROUP.contains(status)
                            ? WAITING_APPROVAL_GROUP.stream()
                            : Stream.of(status))
                    .distinct()
                    .collect(Collectors.toList());

            if (!expanded.isEmpty()) {
                statuses = expanded;
            }
        }
        List<String> priorities = (request.getPriority() != null && !request.getPriority().isEmpty())
                ? request.getPriority() : null;
        List<String> batchIds = (request.getBatchIds() != null && !request.getBatchIds().isEmpty())
                        ? request.getBatchIds() : null;
        List<String> batchTypes = (request.getBatchTypes() != null && !request.getBatchTypes().isEmpty())
                        ? request.getBatchTypes() : null;
        List<Object> motorStages = (request.getMotorStages() != null && !request.getMotorStages().isEmpty())
                        ? request.getMotorStages() : null;
        List<String> motorIds = (request.getMotorIds() != null && !request.getMotorIds().isEmpty())
                        ? request.getMotorIds() : null;
        List<String> formSubmittedBy = (request.getFormSubmittedBy() != null
                && !request.getFormSubmittedBy().isEmpty())
                ? request.getFormSubmittedBy()
                : null;

        LocalDate fromDate = request.getFromDate();
        LocalDate toDate = request.getToDate();

        Pageable pageable = PageRequest.of(Math.max(0, request.getPage() - 1), request.getLimit(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Batch> page = batchRepository.findUserSubDeptBatches(subDeptId, request.getUserId(), request.getSearch(),
                statuses, priorities, batchIds, batchTypes, motorStages, motorIds,formSubmittedBy,
                fromDate,
                toDate,pageable);

        StatusCountsDto statusCounts =
                filterStatusCounts(batchRepository.getStatusCounts(subDeptId), subDeptId);

        boolean isStaticTesting = Objects.equals(subDeptId, STATIC_TESTING_SUB_DEPARTMENT_ID);
        Map<String, List<String>> bemMotorIdsByBatch = isStaticTesting
                ? subscaleMetadataResolver.resolveApprovedBemMotorIds(
                        page.getContent().stream().map(Batch::getBatchId).toList())
                : Collections.emptyMap();

        List<UserBatchItemDto> dtos = page.getContent().stream()
                .map(batch -> mapToUserBatchItem(
                        batch,
                        subDeptId,
                        isStaticTesting
                                ? bemMotorIdsByBatch.getOrDefault(
                                        batch.getBatchId(),
                                        Collections.emptyList())
                                : null))
                .collect(Collectors.toList());

        return UserBatchListResponse.builder()
                .statusCounts(statusCounts)
                .batches(dtos)
                .pagination(PaginationDto.builder()
                        .page(request.getPage())
                        .pageSize(request.getLimit())
                        .totalRecords(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }
    // ───────────────────────── SPECIFICATIONS LIST ────────────────────────
    public SpecificationListResponse getSpecificationList(SpecificationListRequest request) {

        MaterialMaster material = materialRepo
                .findFirstByMaterialCode(request.getMaterialCode())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Material not found: " + request.getMaterialCode()));

        if (!MaterialMasterDataService.isActive(material)) {
            throw new AcemException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Material not found: " + request.getMaterialCode());
        }

        List<MaterialSpecification> specifications = material.getSpecifications();

        List<SpecificationDto> specs = specifications == null
                ? Collections.emptyList()
                : specifications.stream()
                .filter(MaterialMasterDataService::isSpecificationActive)
                .map(spec -> SpecificationDto.builder()
                        .specificationCode(spec.getSpecificationCode())
                        .specificationName(spec.getSpecificationName())
                        .referenceRange(spec.getReferenceRange() == null
                                ? null
                                : SpecificationRangeDto.builder()
                                .minValue(spec.getReferenceRange().getMinValue())
                                .maxValue(spec.getReferenceRange().getMaxValue())
                                .unit(spec.getReferenceRange().getUnit())
                                .build())
                        .build())
                .toList();

        return SpecificationListResponse.builder()
                .materialCode(material.getMaterialCode())
                .grade(null)
                .specifications(specs)
                .build();
    }
    // ───────────────────────── MOTOR LIST BY BATCH────────────────────────
    public MotorListResponse getMotorsByBatch(String batchId) {
        Batch batch = batchRepository.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists for batchId: " + batchId));

        List<String> motorIds = batch.getMotorIds();
        if (motorIds == null || motorIds.isEmpty()) {
            throw new AcemException(ErrorCode.MOTORS_NOT_FOUND,
                    "No motors are associated with the specified batch");
        }

        List<MotorDetailDto> motorDetails = new ArrayList<>();
        for (String motorId : motorIds) {
            // Find the active casing for this motor – no additional status filter
            List<RocketMotorCasing> casings = rmcRepository
                    .findByMotorIdAndProjectIdAndMotorStageAndActiveTrue(
                            motorId,
                            batch.getProjectId(),
                            batch.getMotorStage());

            RocketMotorCasing casing = casings.isEmpty() ? null : casings.get(0);

            String motorCasingId = casing != null ? casing.getMotorCasingId() : null;
            String motorCasingStatus = casing != null ? casing.getStatus().name() : "UNKNOWN";

            String castingStatus = "NOT_STARTED";
            String curingStatus = "NOT_STARTED";

            motorDetails.add(MotorDetailDto.builder()
                    .motorCasingId(motorCasingId)
                    .motorId(motorId)
                    .motorCasingStatus(motorCasingStatus)
                    .castingStatus(castingStatus)
                    .curingStatus(curingStatus)
                    .build());
        }

        return MotorListResponse.builder()
                .batchId(batch.getBatchId())
                .batchType(batch.getBatchType())
                .projectId(batch.getProjectId())
                .motorStage(batch.getMotorStage())
                .motors(motorDetails)
                .build();
    }
    // ───────────────────────── MATERIAL LIST BY BATCH ────────────────────────
    public MaterialLotResponse getMaterialLotsByBatch(String batchId) {
        // 1. Fetch batch
        Batch batch = batchRepository.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND,
                        "No batch exists with batchId: " + batchId));

        // 2. Extract materials from identification sheet
        List<BatchMaterial> batchMaterials = batch.getIdentificationSheet() != null
                ? batch.getIdentificationSheet().getMaterials()
                : null;

        if (batchMaterials == null || batchMaterials.isEmpty()) {
            throw new AcemException(ErrorCode.MATERIAL_LOTS_NOT_FOUND,
                    "No material lot information available for the selected batch");
        }

        // 3. Enrich with materialName from master data
        List<MaterialLotDto> materialDtos = batchMaterials.stream()
                .map(bm -> {
                    String materialName = materialRepo.findFirstByMaterialCode(bm.getMaterialCode())
                            .map(MaterialMaster::getMaterialName)
                            .orElse(bm.getMaterialCode());   // fallback to code if not found
                    return MaterialLotDto.builder()
                            .materialCode(bm.getMaterialCode())
                            .grade(bm.getGrade())
                            .materialName(materialName)
                            .lotId(bm.getLotIds()) // Updated from setLotId(bm.getLotId()) to handle the list
                            .make(bm.getMake())
                            .build();
                })
                .collect(Collectors.toList());

        return MaterialLotResponse.builder()
                .batchId(batch.getBatchId())
                .materials(materialDtos)
                .build();
    }
    // ───────────────────────── DIMENSIONAL PARAMETERS LIST ────────────────────────
    public DimensionalParamListResponse getDimensionalParams(DimensionalParamRequest request) {
        String projectId = request.getProjectId() == null ? "" : request.getProjectId().trim();
        List<DimensionalParamDto> params = dimParamRepo
                .findByProjectIdAndMotorType(projectId, request.getMotorType())
                .stream()
                .filter(DimensionalParameterMasterDataHandler::isActive)
                .sorted(Comparator.comparing(DimensionalParameter::getParamId, Comparator.nullsLast(String::compareTo)))
                .map(p -> DimensionalParamDto.builder()
                        .paramId(p.getParamId())
                        .paramName(p.getParamName())
                        .referenceRange(SpecificationRangeDto.builder()
                                .minValue(p.getMinValue())
                                .maxValue(p.getMaxValue())
                                .unit(p.getUnit())
                                .build())
                        .build())
                .collect(Collectors.toList());

        return DimensionalParamListResponse.builder()
                .motorType(request.getMotorType())
                .parameters(params)
                .build();
    }

    public List<MotorStageDto> getMotorStageList(String projectId) {
        List<MotorStage> stages = projectId != null && !projectId.isBlank()
                ? motorStageRepository.findByProjectIdOrderByMotorStageAsc(projectId.trim())
                : motorStageRepository.findAllByOrderByMotorStageAsc();
        return stages.stream()
                .filter(MotorStageMasterDataHandler::isActive)
                .map(stage -> MotorStageDto.builder()
                        .motorStage(stage.getMotorStage())
                        .build())
                .toList();
    }

    public RocketMotorCasingListResponse getCasingList(RocketMotorCasingListRequest request) {
        Query query = new Query();

        // MANDATORY FILTERS
        query.addCriteria(Criteria.where("subDepartmentId").is(request.getSubDepartmentId()));
        query.addCriteria(Criteria.where("active").is(true));

        // OPTIONAL FILTERS
        // STATUS FILTER
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            query.addCriteria(Criteria.where("status").in(request.getStatus()));
        }

        // MOTOR STAGE FILTER
        if (request.getMotorStage() != null && !request.getMotorStage().isEmpty()) {
            query.addCriteria(Criteria.where("motorStage").in(request.getMotorStage()));
        }
        // PROJECT ID FILTER
        if (request.getProjectId() != null && !request.getProjectId().isEmpty()) {
            query.addCriteria(Criteria.where("projectId").in(request.getProjectId()));
        }
        // CASING TYPE FILTER
        if (request.getCasingType() != null && !request.getCasingType().isEmpty()) {
            query.addCriteria(Criteria.where("sections.motorReceipt.casingType").in(request.getCasingType()));
        }

        // INSULATION TYPE FILTER
        if (request.getInsulationType() != null && !request.getInsulationType().isEmpty()) {
            query.addCriteria(Criteria.where("sections.motorReceipt.insulation.type").in(request.getInsulationType()));
        }

        // SEARCH FILTER
        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            String search = request.getSearch().trim();
            query.addCriteria(new Criteria().orOperator(
                            Criteria.where("motorCasingId").regex(search, "i"),
                            Criteria.where("motorId").regex(search, "i"),
                            Criteria.where("projectId").regex(search, "i"),
                            Criteria.where("sections.motorReceipt.clearances.greenCardNo").regex(search, "i")
                    )
            );
        }

        // DATE RANGE FILTER
        if (request.getFromDate() != null && request.getToDate() != null) {
            if (request.getFromDate().isAfter(request.getToDate())) {
                throw new AcemException(
                        ErrorCode.INVALID_DATE_RANGE,
                        "Invalid date range",
                        "fromDate cannot be after toDate"
                );
            }
            // Filter by Created On (entity createdAt), matching the list Created On column.
            query.addCriteria(Criteria.where("createdAt")
                            .gte(request.getFromDate().atStartOfDay())
                            .lte(request.getToDate().atTime(23, 59, 59))
            );
        }

        // TOTAL COUNT
        long totalRecords = mongoTemplate.count(query, RocketMotorCasing.class);


        // PAGINATION & SORTING
        int page = Math.max(request.getPage(), 1);
        int limit = Math.max(request.getLimit(), 1);
        query.with(PageRequest.of(page - 1, limit));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));

        // FETCH DATA
        List<RocketMotorCasing> entities = mongoTemplate.find(query, RocketMotorCasing.class);

        // MAP RESPONSE
        List<CasingResponse> casings = entities.stream()
                        .map(this::mapToResponse)
                        .toList();

        // STATUS COUNTS
        StatusCounts counts = buildStatusCounts(request);
        return RocketMotorCasingListResponse.builder()
                .statusCounts(counts)
                .casings(casings)
                .pagination(
                        PaginationDto.builder()
                                .page(page)
                                .pageSize(limit)
                                .totalRecords(totalRecords)
                                .totalPages((int) Math.ceil((double) totalRecords / limit))
                                .build())
                .build();
    }

    public List<InsulationTypeDto> getInsulationTypeList() {
        return insRepository.findAll().stream()
                .filter(InsulationSpecificationMasterDataService::isActive)
                .filter(entity -> entity.getInsulationType() != null
                        && !entity.getInsulationType().isBlank())
                .sorted(Comparator.comparing(
                        InsulationSpecification::getInsulationType,
                        String.CASE_INSENSITIVE_ORDER))
                .map(entity -> InsulationTypeDto.builder()
                        .insulationSpecId(entity.getInsulationSpecId())
                        .insulationType(entity.getInsulationType())
                        .build())
                .toList();
    }

    public InsulationSpecificationResponse getInsulationSpecifications(
            InsulationSpecificationRequest request) {

        InsulationSpecification entity = insRepository
                .findByInsulationTypeIgnoreCase(request.getInsulationType())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "No specifications available for selected insulation type"
                ));

        if (!InsulationSpecificationMasterDataService.isActive(entity)) {
            throw new AcemException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "No specifications available for selected insulation type"
            );
        }

        return InsulationSpecificationResponse.builder()
                .insulationType(entity.getInsulationType())
                .specifications(InsulationSpecificationMasterDataService.filterActiveSpecifications(
                        entity.getSpecifications()))
                .build();
    }
    private StatusCountsDto filterStatusCounts(StatusCountsDto counts, Long subDeptId) {
        if (subDeptId == 1L || subDeptId == 2L) {
            counts.setWaitingForPartialApproval(null);
            counts.setWaitingForCompleteApproval(null);
            counts.setFinalApprovalCompleted(null);
        }else if(subDeptId == 8){
            counts.setWaitingForPartialApproval(null);
            counts.setApproved(null);
        }
        else {
            counts.setApproved(null);
            counts.setWaitingForApproval(null);
        }
        return counts;
    }
    private UserBatchItemDto mapToUserBatchItem(
            Batch batch,
            Long subDeptId,
            List<String> bemMotorIds) {
        // Get the matching stage progress entry
        List<Batch.StageProgress> stageProgress = stageProgressDisplayEnricher.enrichCopies(
                batch,
                batch.getStageProgress() != null
                        ? batch.getStageProgress()
                        : Collections.emptyList());
        List<Batch.StageProgress> currentStage = stageProgressDisplayEnricher.enrichCopies(
                batch,
                batch.getCurrentStage() != null
                        ? batch.getCurrentStage()
                        : Collections.emptyList());
        Batch.StageProgress sp = stageProgress.stream()
                .filter(s -> Objects.equals(s.getSubDepartmentId(), subDeptId))
                .findFirst().orElse(null);

        String rejectionReason = (sp != null && sp.getStatus() == Status.REJECTED) ? sp.getRejectionReason() : null;

        PersonInfo formSubmittedBy = sp != null ? sp.getSubmittedBy() : null;
        LocalDateTime submittedAt = sp != null ? sp.getSubmittedOn() : null;
        // Resolve form ID from the sub-department's form repository
        String formId = batchFormResolver.resolveFormId(batch.getBatchId(), subDeptId);

        // lotIds from identification sheet
        List<String> lotIds = Collections.emptyList();
        if (batch.getIdentificationSheet() != null && batch.getIdentificationSheet().getMaterials() != null) {
            lotIds = batch.getIdentificationSheet().getMaterials().stream()
                    .filter(mat -> mat.getLotIds() != null)
                    .flatMap(mat -> mat.getLotIds().stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }

        PersonInfo systemManager = batch.getSystemManager() != null
                ? PersonInfo.builder().id(batch.getSystemManager().getId()).fullName(batch.getSystemManager().getFullName()).build()
                : null;
        PersonInfo createdBy = batch.getCreatedBy() != null
                ? PersonInfo.builder().id(batch.getCreatedBy().getId()).fullName(batch.getCreatedBy().getFullName()).build()
                : null;

        return UserBatchItemDto.builder()
                .id(batch.getBatchId())
                .formId(formId)
                .batchId(batch.getBatchId())
                .motorIds(batch.getMotorIds())
                .bemMotorIds(bemMotorIds)
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
                .systemManager(systemManager)
                .formSubmittedBy(formSubmittedBy)
                .subDepartmentId(subDeptId)
                .currentStage(currentStage)
                .stageProgress(stageProgress)
                .identificationSheetStatus(batch.getIdentificationSheetStatus())
                .createdOn(batch.getCreatedAt())
                .createdBy(createdBy)
                .submittedAt(submittedAt)
                .rejectionReason(rejectionReason)
                .build();
    }

    private CasingResponse mapToResponse(RocketMotorCasing entity) {
        Sections sections = entity.getSections();
        MotorReceipt receipt = sections != null ? sections.getMotorReceipt() : null;
        Insulation insulation = receipt != null ? receipt.getInsulation() : null;
        return CasingResponse.builder().motorCasingId(entity.getMotorCasingId())
                .motorStage(entity.getMotorStage())
                .project(
                        ProjectDto.builder()
                                .projectId(entity.getProjectId())
                                .projectName(entity.getProjectName())
                                .build()
                )
                .motorId(entity.getMotorId())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdOn(entity.getCreatedAt())
                .createdBy(PersonInfo.builder()
                        .id(entity.getCreatedByUserId())
                        .fullName(entity.getCreatedByUsername())   // replace with real user lookup
                        .build())
                .casingType(receipt != null ? receipt.getCasingType() : null)
                .insulationType(insulation != null && insulation.getType() != null && !insulation.getType().isBlank()
                        ? insulation.getType() : null)
                .receivingDate(receipt != null ? receipt.getReceivingDate() : null)
                .build();
    }

    private StatusCounts buildStatusCounts(RocketMotorCasingListRequest request) {
        MatchOperation match = Aggregation.match(new Criteria().andOperator(
                        Criteria.where("subDepartmentId")
                                .is(request.getSubDepartmentId()),
                        Criteria.where("status")
                                .ne(Status.DELETED)
                )
        );
        GroupOperation group = Aggregation.group("status").count().as("count");
        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<Document> result = mongoTemplate.aggregate(aggregation, RocketMotorCasing.class,
                Document.class);
        Map<String, Long> map = new HashMap<>();
        for (Document doc : result.getMappedResults()) {
            String status = doc.get("_id").toString();
            long count = ((Number) doc.get("count")).longValue();map.put(status, count);
        }
        return StatusCounts.builder()
                .initiated(map.getOrDefault(Status.TO_BE_INITIATED.name(), 0L))
                .inProgress(map.getOrDefault(Status.IN_PROGRESS.name(), 0L))
                .waitingForApproval(map.getOrDefault(Status.WAITING_FOR_APPROVAL.name(), 0L))
                .approved(map.getOrDefault(Status.APPROVED.name(), 0L))
                .rejected(map.getOrDefault(Status.REJECTED.name(), 0L))
                .build();
    }

    public List<CastingStationDto> getCastingStations() {
        return castingStationRepository
                .findByStatusOrderByStationIdAsc("ACTIVE")
                .stream()
                .map(station -> CastingStationDto.builder()
                        .stationId(station.getStationId())
                        .stationCode(station.getStationCode())
                        .stationName(station.getStationName())
                        .stationType(station.getStationType())
                        .status(station.getStatus())
                        .build())
                .toList();
    }

    private boolean matchesRawMaterialCategory(MaterialMaster material, String rawMaterialType) {
        if (rawMaterialType == null || rawMaterialType.isBlank()) {
            return true;
        }
        try {
            return MaterialMasterDataService.resolveRawMaterialCategory(material.getRawMaterialType())
                    == RawMaterialCategory.fromValue(rawMaterialType);
        } catch (IllegalArgumentException ex) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid request",
                    "rawMaterialType must be NORMAL or ACEM");
        }
    }

}
