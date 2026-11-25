package in.co.bel.acempp.service.qualitycontrol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.co.bel.acempp.dto.castingcuring.CastingCuringFormResponse;
import in.co.bel.acempp.dto.general.ApproverStatusRequest;
import in.co.bel.acempp.dto.general.ApproverStatusResponse;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.casepreparation.*;
import in.co.bel.acempp.dto.castingcuring.CastingCuringMotor;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.*;
import in.co.bel.acempp.dto.mixing.*;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingDetailsDto;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormRequest;
import in.co.bel.acempp.dto.mixing.mixingform.MixingFormResponse;
import in.co.bel.acempp.dto.mixing.mixingform.PremixDto;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParameterDto;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.dto.ndt.NdtResponse;
import in.co.bel.acempp.dto.postcureoperations.PostCureMotor;
import in.co.bel.acempp.dto.postcureoperations.PostCureResponse;
import in.co.bel.acempp.dto.rawmaterialpreparation.*;
import in.co.bel.acempp.dto.ndt.NdtMotorDto;
import in.co.bel.acempp.dto.ndt.motor.RadiographyDetailsDto;
import in.co.bel.acempp.dto.ndt.motor.RadiographyPlanDetailsDto;
import in.co.bel.acempp.dto.file.FileBindingScope;
import in.co.bel.acempp.dto.qcdivision.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotDetailsRequest;
import in.co.bel.acempp.dto.rawmaterialsourcing.LotDetailsResponse;
import in.co.bel.acempp.dto.rawmaterialsourcing.SpecificationResponse;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.dto.statictestingfacility.StaticMotorData;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.RadiographyDetails;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.Sections;
import in.co.bel.acempp.dto.statictestingfacility.StfResponse;
import in.co.bel.acempp.dto.subscale.SubScaleMetadata;
import in.co.bel.acempp.dto.subscale.subscalecreation.CuringDetailsDTO;
import in.co.bel.acempp.dto.subscale.subscalecreation.HardwarePreparationDetailsDTO;
import in.co.bel.acempp.entity.sourcing.RocketMotorCasing;
import in.co.bel.acempp.dto.trimming.TrimmingDetailsResponse;
import in.co.bel.acempp.dto.user.operations.SpecificationRangeDto;
import in.co.bel.acempp.entity.batch.Batch;
import in.co.bel.acempp.entity.batch.BatchMaterial;
import in.co.bel.acempp.entity.FileAttachments;
import in.co.bel.acempp.entity.manufacturing.*;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.entity.qualitycontrol.DivisionDetail;
import in.co.bel.acempp.entity.qualitycontrol.QcDivision;
import in.co.bel.acempp.entity.qualitycontrol.QcDivisionMaster;
import in.co.bel.acempp.entity.qualitycontrol.QcDivisionWorkflow;
import in.co.bel.acempp.entity.User;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.StageType;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.BatchRepository;
import in.co.bel.acempp.repository.manufacturing.*;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.qualitycontrol.NdtRepository;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionMasterRepository;
import in.co.bel.acempp.repository.qualitycontrol.QcDivisionRepository;
import in.co.bel.acempp.repository.qualitycontrol.StaticTestingRepository;
import in.co.bel.acempp.repository.sourcing.RocketMotorCasingRepository;
import in.co.bel.acempp.repository.UserRepository;
import in.co.bel.acempp.service.*;
import in.co.bel.acempp.service.batch.MainBatchGateValidator;
import in.co.bel.acempp.service.batch.MainBatchStageConstants;
import in.co.bel.acempp.service.batch.MainBatchStageProgressHelper;
import in.co.bel.acempp.service.batch.QcDivisionProgressHelper;
import in.co.bel.acempp.validation.custom.ValidateQcPostCureMotor;
import in.co.bel.acempp.util.FileIdExtractionUtil;
import in.co.bel.acempp.service.manufacturing.*;
import in.co.bel.acempp.service.masterdata.CuringCycleMasterDataService;
import in.co.bel.acempp.service.sourcing.RawMaterialSourcingService;
import in.co.bel.acempp.util.ApprovalStatusUtil;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import in.co.bel.acempp.util.SpecResultFormatValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class QcDivisionService {

    private static final long RAW_MATERIAL_REVALIDATION_DIVISION_ID = 101L;
    private static final long RAW_MATERIAL_PROCESSING_DIVISION_ID = 102L;
    private static final long MIXING_DIVISION_ID = 2L;
    private static final long HARDWARE_DIVISION_ID = 3L;
    private static final long CASTING_DIVISION_ID = 4L;
    private static final long CURING_DIVISION_ID = 5L;
    private static final long DE_CORING_DIVISION_ID = 6L;
    private static final long TRIMMING_DIVISION_ID = 7L;
    private static final long POST_CURE_DIVISION_ID = 8L;
    private static final long NDT_DIVISION_ID = 9L;
    private static final long STATIC_TEST_FACILITY_DIVISION_ID = 12L;
    private static final String CASTING_SECTIONS = "castingSections";
    private static final String CURING_SECTIONS = "curingSections";
    private static final Set<String> PREMIX_DIVISIONS = Set.of(
            "RAW_MATERIAL_PROCESSING", "MIXING");
    private static final Set<String> DIVISION_SCOPED_DIVISIONS = Set.of(
            "RAW_MATERIAL_REVALIDATION");
    private static final Set<String> MOTOR_DIVISIONS = Set.of(
            "HARDWARE", "CASTING", "CURING", "DE_CORING", "TRIMMING",
            "POST_CURE", "NDT", "QC", "WEIGHTMENT", "STATIC_TEST_FACILITY");
    private static final Map<String, UnitShape> MOTOR_UNIT_SHAPES = Map.ofEntries(
            Map.entry("HARDWARE", new UnitShape("motorDetails", "motorId")),
            Map.entry("CASTING", new UnitShape("motorDetails", "motorId")),
            Map.entry("CURING", new UnitShape("curingDetails", "motorId")),
            Map.entry("DE_CORING", new UnitShape("deCoringDetails", "motorId")),
            Map.entry("TRIMMING", new UnitShape("trimmingDetails", "motorIdNo")),
            Map.entry("POST_CURE", new UnitShape("postCureMotorDetails", "motorId")),
            Map.entry("NDT", new UnitShape("motorDetails", "motorId")),
            Map.entry("QC", new UnitShape("motors", "motorId")),
            Map.entry("WEIGHTMENT", new UnitShape("motorWeightDetails", "motorId")),
            Map.entry("STATIC_TEST_FACILITY", new UnitShape("motors", "motorId")));
    private static final Map<String, Long> QC_DIVISION_SOURCE_SUB_DEPT = Map.ofEntries(
            Map.entry("RAW_MATERIAL_REVALIDATION", 1L),
            Map.entry("RAW_MATERIAL_PROCESSING", 3L),
            Map.entry("MIXING", 5L),
            Map.entry("HARDWARE", 4L),
            Map.entry("CASTING", 6L),
            Map.entry("CURING", 6L),
            Map.entry("DE_CORING", 6L),
            Map.entry("TRIMMING", 9L),
            Map.entry("POST_CURE", 7L),
            Map.entry("NDT", 10L),
            Map.entry("QC", 5L),
            Map.entry("WEIGHTMENT", 11L),
            Map.entry("STATIC_TEST_FACILITY", 12L));

    private final QcDivisionRepository qcDivisionRepo;
    private final QcDivisionMasterRepository qcDivisionMasterRepo;
    private final BatchRepository batchRepo;
    private final MaterialRepository materialRepo;
    private final RawMaterialPreparationRepository rawMaterialPreparationRepo;
    private final MixingRepository mixingRepo;
    private final QualityCheckRepository qualityCheckRepo;
    private final MixingCycleRepository mixingCycleRepo;
    private final CasePreparationRepository casePreparationRepo;
    private final CastingCuringRepository castingCuringRepo;
    private final CuringCycleMasterRepository curingCycleMasterRepo;
    private final SubscaleProcessingRepository subscaleProcessingRepo;
    private final TrimmingRepository trimmingRepo;
    private final PostCureRepository postCureRepo;
    private final NdtRepository ndtRepo;
    private final StaticTestingRepository staticTestingRepo;
    private final StaticTestingService staticTestingService;
    private final RocketMotorCasingRepository rmcRepository;
    private final UserRepository userRepo;
    private final BatchService batchService;
    private final SubscaleMetadataResolver subscaleMetadataResolver;
    private final QcDivisionStageStatusResolver qcDivisionStageStatusResolver;
    private final FormIdGeneratorService formIdGenerator;
    private final RawMaterialPrepService rawMaterialPrepService;
    private final MixingService mixingService;
    private final CasePreparationService casePreparationService;
    private final CastingCuringService castingCuringService;
    private final TrimmingService trimmingService;
    private final PostCureOptService postCureOptService;
    private final NdtService ndtService;
    private final RawMaterialSourcingService rawMaterialSourcingService;
    private final QcDivisionPayloadMapper qcDivisionPayloadMapper;
    private final ObjectMapper objectMapper;
    private final MainBatchGateValidator mainBatchGateValidator;
    private final FileBindingService fileBindingService;
    private final FileAttachmentLookupService fileAttachmentLookupService;
    private final FileReadOnlyService fileReadOnlyService;
    private final FileMetadataEnrichmentService fileMetadataEnrichmentService;
    private final QcHardwareMapper qcHardwareMapper;
    private final QcNdtMapper qcNdtMapper;
    private final ValidateQcPostCureMotor validateQcPostCureMotor;
    private final ApprovalBlockchainAssetService approvalBlockchainAssetService;
    private boolean finalSubmit;

    public List<QcDivisionOptionDto> getDivisionOptions() {
        List<QcDivisionMaster> divisions = qcDivisionMasterRepo.findAllByOrderByDisplayOrderAsc();
        if (divisions.isEmpty()) {
            throw new AcemException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "QC division master data not found");
        }

        Comparator<QcDivisionMaster.DivisionType> typeOrder = Comparator.comparing(
                QcDivisionMaster.DivisionType::getDisplayOrder,
                Comparator.nullsLast(Comparator.naturalOrder()));

        return divisions.stream()
                .map(division -> QcDivisionOptionDto.builder()
                        .divisionId(division.getDivisionId())
                        .divisionName(division.getDivisionName())
                        .disabled(Boolean.TRUE.equals(division.getDisabled()))
                        .types(Optional.ofNullable(division.getTypes())
                                .orElseGet(Collections::emptyList)
                                .stream()
                                .sorted(typeOrder)
                                .map(type -> QcDivisionOptionDto.DivisionTypeOptionDto.builder()
                                        .divisionId(type.getDivisionId())
                                        .divisionName(type.getDivisionName())
                                        .disabled(Boolean.TRUE.equals(type.getDisabled()))
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    public Map<String, Object> autoPopulateDivision(
            QcDivisionAutoPopulateRequest request) {

        ResolvedDivision selectedDivision = resolveDivision(request.getDivisionId());
        if (selectedDivision.disabled()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "QC division is disabled: " + selectedDivision.divisionName());
        }
        if (!Objects.equals(selectedDivision.divisionId(), RAW_MATERIAL_REVALIDATION_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), RAW_MATERIAL_PROCESSING_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), MIXING_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), HARDWARE_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), CASTING_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), CURING_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), DE_CORING_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), TRIMMING_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), POST_CURE_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), NDT_DIVISION_ID)
                && !Objects.equals(selectedDivision.divisionId(), STATIC_TEST_FACILITY_DIVISION_ID)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Auto-population is not supported for QC division: "
                            + selectedDivision.divisionName());
        }

        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(request.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND,
                        "Batch not found: " + request.getBatchId()));

        Map<String, Object> selectedDivisionData = new LinkedHashMap<>();
        selectedDivisionData.put("divisionId", selectedDivision.divisionId());
        selectedDivisionData.put("divisionName", selectedDivision.divisionName());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("batchId", batch.getBatchId());
        response.put("selectedDivision", selectedDivisionData);

        if (Objects.equals(selectedDivision.divisionId(), RAW_MATERIAL_PROCESSING_DIVISION_ID)) {
            validatePremixConfiguration(batch);
            Map<String, Object> processingSeed = loadRawMaterialProcessingDivisionSeed(
                    selectedDivision.divisionName(),
                    batch.getBatchId());
            response.put("premixes", processingSeed.getOrDefault("premixes", Collections.emptyList()));
            Object weightmentSheet = processingSeed.get("weightmentSheet");
            if (weightmentSheet != null) {
                response.put("weightmentSheet", weightmentSheet);
            }
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), MIXING_DIVISION_ID)) {
            validatePremixConfiguration(batch);
            response.put("stages", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildMixingStages(batch)));
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), HARDWARE_DIVISION_ID)) {
            response.put("motors", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildHardwareMotors(batch)));
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), CASTING_DIVISION_ID)
                || Objects.equals(selectedDivision.divisionId(), CURING_DIVISION_ID)) {
            String sectionKey = Objects.equals(
                    selectedDivision.divisionId(),
                    CASTING_DIVISION_ID)
                    ? CASTING_SECTIONS
                    : CURING_SECTIONS;
            response.put("motors", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildCastingCuringMotors(batch, sectionKey)));
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), DE_CORING_DIVISION_ID)) {
            response.put("motors", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildDeCoringMotors(batch.getBatchId())));
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), TRIMMING_DIVISION_ID)) {
            response.put("motors", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildTrimmingMotors(batch.getBatchId())));
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), POST_CURE_DIVISION_ID)) {
            response.put("motors", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildPostCureMotors(batch.getBatchId())));
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), NDT_DIVISION_ID)) {
            response.put("motors", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildNdtMotors(batch.getBatchId())));
            return response;
        }

        if (Objects.equals(selectedDivision.divisionId(), STATIC_TEST_FACILITY_DIVISION_ID)) {
            response.put("motors", loadDivisionDetailsOrEmpty(
                    selectedDivision.divisionName(),
                    batch.getBatchId(),
                    () -> buildStaticTestFacilityMotors(batch.getBatchId())));
            return response;
        }

        response.put("materials", loadDivisionDetailsOrEmpty(
                selectedDivision.divisionName(),
                batch.getBatchId(),
                () -> buildRawMaterialRevalidationMaterials(batch)));
        return response;
    }

    private List<Map<String, Object>> buildRawMaterialRevalidationMaterials(Batch batch) {

        if (batch.getIdentificationSheet() == null
                || batch.getIdentificationSheet().getMaterials() == null
                || batch.getIdentificationSheet().getMaterials().isEmpty()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Identification sheet materials are not available for the batch");
        }

        List<BatchMaterial> batchMaterials = batch.getIdentificationSheet().getMaterials().stream()
                .filter(Objects::nonNull)
                .toList();
        if (batchMaterials.isEmpty()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Identification sheet materials are not available for the batch");
        }

        Map<String, MaterialMaster> mastersByCode = materialRepo.findByMaterialCodeIn(
                        batchMaterials.stream()
                                .map(BatchMaterial::getMaterialCode)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList())
                .stream()
                .filter(material -> material.getMaterialCode() != null)
                .collect(Collectors.toMap(
                        MaterialMaster::getMaterialCode,
                        material -> material,
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        // Revalidation lists every identification-sheet material that has lot IDs,
        // once per lot, with that material master's own specs.
        Map<String, LinkedHashSet<String>> lotsByCode = new LinkedHashMap<>();
        for (BatchMaterial material : batchMaterials) {
            MaterialMaster master = mastersByCode.get(material.getMaterialCode());
            if (master == null || material.getLotIds() == null) {
                continue;
            }
            LinkedHashSet<String> lots = lotsByCode.computeIfAbsent(
                    material.getMaterialCode(), key -> new LinkedHashSet<>());
            for (String lotId : material.getLotIds()) {
                if (lotId != null && !lotId.isBlank()) {
                    lots.add(lotId.trim());
                }
            }
        }

        List<Map<String, Object>> materials = new ArrayList<>();
        int srNo = 1;
        for (Map.Entry<String, LinkedHashSet<String>> entry : lotsByCode.entrySet()) {
            MaterialMaster master = mastersByCode.get(entry.getKey());
            if (master == null || entry.getValue().isEmpty()) {
                continue;
            }
            List<Map<String, Object>> specifications = topLevelRevalidationSpecifications(master);
            for (String lotId : entry.getValue()) {
                materials.add(toApRevalidationLot(master, srNo++, lotId, specifications));
            }
        }
        return materials;
    }

    private List<Map<String, Object>> topLevelRevalidationSpecifications(MaterialMaster master) {
        if (master.getSpecifications() == null) {
            return List.of();
        }
        List<Map<String, Object>> specifications = new ArrayList<>();
        for (MaterialSpecification specification : master.getSpecifications()) {
            if (specification == null || Boolean.FALSE.equals(specification.getIsActive())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("specificationCode", specification.getSpecificationCode());
            row.put("specificationName", specification.getSpecificationName());
            row.put("referenceRange", specification.getReferenceRange());
            specifications.add(row);
        }
        return specifications;
    }

    private <T> List<T> loadDivisionDetailsOrEmpty(
            String divisionName,
            String batchId,
            Supplier<List<T>> detailsLoader) {
        try {
            List<T> details = detailsLoader.get();
            return details != null ? details : Collections.emptyList();
        } catch (AcemException ex) {
            if (ex.getErrorCode().getStatus() != HttpStatus.NOT_FOUND) {
                throw ex;
            }
            log.debug(
                    "Skipping {} auto-population for batch {} because source details were not found: {}",
                    divisionName,
                    batchId,
                    ex.getMessage());
            return Collections.emptyList();
        }
    }

    private void validatePremixConfiguration(Batch batch) {
        if (batch.getIdentificationSheet() == null
                || batch.getIdentificationSheet().getNumberOfPremix() == null
                || batch.getIdentificationSheet().getNumberOfPremix() <= 0) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Required premix configuration is not available for batch: "
                            + batch.getBatchId());
        }
    }

    /**
     * Loads RMP premixes + shared weightment sheet for QC Raw Material Processing auto-populate.
     * Weightment is returned so QC can show RMP weighment UI when a material has no QC schema.
     */
    private Map<String, Object> loadRawMaterialProcessingDivisionSeed(
            String divisionName,
            String batchId) {
        try {
            return buildRawMaterialProcessingDivisionSeed(batchId);
        } catch (AcemException ex) {
            if (ex.getErrorCode().getStatus() != HttpStatus.NOT_FOUND) {
                throw ex;
            }
            log.debug(
                    "Skipping {} auto-population for batch {} because source details were not found: {}",
                    divisionName,
                    batchId,
                    ex.getMessage());
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("premixes", Collections.emptyList());
            return empty;
        }
    }

    private Map<String, Object> buildRawMaterialProcessingDivisionSeed(String batchId) {
        String formId = rawMaterialPreparationRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Raw material preparation form not found for batch: " + batchId))
                .getFormId();

        PreparationFormDetailsResponse formDetails = rawMaterialPrepService.getFormDetails(formId);
        if (formDetails.getPremixStatuses() == null || formDetails.getPremixStatuses().isEmpty()) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Premix status details are not available for batch: " + batchId);
        }

        List<PremixRequest> savedPremixes = formDetails.getPreparationDetails() != null
                && formDetails.getPreparationDetails().getPremixes() != null
                ? formDetails.getPreparationDetails().getPremixes()
                : Collections.emptyList();
        Map<Integer, PremixRequest> premixesByNumber = savedPremixes.stream()
                .collect(Collectors.toMap(
                        PremixRequest::getPremixNo,
                        premix -> premix,
                        (first, ignored) -> first));

        List<Map<String, Object>> premixes = formDetails.getPremixStatuses().stream()
                .sorted(Comparator.comparingInt(PremixStatusResponse::getPremixNo))
                .map(status -> mapPremixByApproval(status, premixesByNumber))
                .toList();

        Map<String, Object> seed = new LinkedHashMap<>();
        seed.put("premixes", premixes);
        WeightmentSheetRequest weightmentSheet = formDetails.getPreparationDetails() != null
                ? formDetails.getPreparationDetails().getWeightmentSheet()
                : null;
        if (weightmentSheet != null) {
            seed.put("weightmentSheet", weightmentSheet);
        }
        return seed;
    }

    private List<Map<String, Object>> buildRawMaterialProcessingPremixes(String batchId) {
        Object premixes = buildRawMaterialProcessingDivisionSeed(batchId).get("premixes");
        if (premixes instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> typed = (List<Map<String, Object>>) list;
            return typed;
        }
        return Collections.emptyList();
    }

    private Map<String, Object> mapPremixByApproval(
            PremixStatusResponse premixStatus,
            Map<Integer, PremixRequest> premixesByNumber) {

        Map<String, Object> premixData = new LinkedHashMap<>();
        premixData.put("premixNo", premixStatus.getPremixNo());
        premixData.put("premixSubmissionStatus", premixStatus.getPremixSubmissionStatus());

        if (premixStatus.getPremixSubmissionStatus() == Status.APPROVED) {
            PremixRequest approvedPremix = premixesByNumber.get(premixStatus.getPremixNo());
            if (approvedPremix == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Approved premix details are not available for premix: "
                                + premixStatus.getPremixNo());
            }
            premixData.put("details", approvedPremix);
        }

        return premixData;
    }

    private List<Map<String, Object>> buildMixingStages(Batch batch) {
        String batchId = batch.getBatchId();
        String formId = mixingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Mixing form not found for batch: " + batchId))
                .getFormId();

        MixingFormRequest detailsRequest = new MixingFormRequest();
        detailsRequest.setFormId(formId);
        MixingFormResponse formDetails = mixingService.getFormDetails(detailsRequest);

        return List.of(
                mapMixingStage(
                        StageType.PREMIX,
                        formDetails.getPremixStatuses(),
                        indexMixingPremixes(formDetails.getMixingDetails(), StageType.PREMIX)),
                mapMixingStage(
                        StageType.FINAL_MIX,
                        formDetails.getFinalMixStatuses(),
                        indexMixingPremixes(formDetails.getMixingDetails(), StageType.FINAL_MIX))
        );
    }

    private Map<String, QualityCheckParams> loadQualityChecksByIds(
            Collection<String> parameterIds) {
        if (parameterIds == null || parameterIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, QualityCheckParams> byId = new LinkedHashMap<>();
        for (MixingCycleMaster cycle : mixingCycleRepo.findAll()) {
            if (cycle == null || cycle.getCycles() == null) {
                continue;
            }
            Stream.of(
                            cycle.getCycles().getPremixQualityChecks(),
                            cycle.getCycles().getFinalMixQualityChecks())
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .filter(parameter -> parameter.getParameterId() != null
                            && parameterIds.contains(parameter.getParameterId()))
                    .forEach(parameter -> byId.putIfAbsent(parameter.getParameterId(), parameter));
        }
        qualityCheckRepo.findByQualityCheckParameterIds(parameterIds).stream()
                .filter(Objects::nonNull)
                .map(QualityCheckMaster::getQualityChecks)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(parameter -> parameter.getParameterId() != null
                        && parameterIds.contains(parameter.getParameterId()))
                .forEach(parameter -> byId.putIfAbsent(parameter.getParameterId(), parameter));
        return byId;
    }

    private Map<Integer, PremixDto> indexMixingPremixes(
            MixingDetailsDto mixingDetails,
            StageType stageType) {

        if (mixingDetails == null || mixingDetails.getStages() == null) {
            return Collections.emptyMap();
        }

        return mixingDetails.getStages().stream()
                .filter(Objects::nonNull)
                .filter(stage -> stage.getStageType() == stageType)
                .filter(stage -> stage.getPremixes() != null)
                .flatMap(stage -> stage.getPremixes().stream())
                .filter(Objects::nonNull)
                .filter(premix -> premix.getPremixNo() != null)
                .collect(Collectors.toMap(
                        PremixDto::getPremixNo,
                        premix -> premix,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private Map<String, Object> mapMixingStage(
            StageType stageType,
            List<PremixStatusResponse> statuses,
            Map<Integer, PremixDto> premixesByNumber) {

        Map<String, Object> stageData = new LinkedHashMap<>();
        stageData.put("stageType", stageType);
        stageData.put("premixes", Optional.ofNullable(statuses)
                .orElseGet(Collections::emptyList)
                .stream()
                .sorted(Comparator.comparing(
                        PremixStatusResponse::getPremixNo,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(status -> mapMixingPremixByApproval(
                        stageType,
                        status,
                        premixesByNumber))
                .toList());
        return stageData;
    }

    private Map<String, Object> mapMixingPremixByApproval(
            StageType stageType,
            PremixStatusResponse premixStatus,
            Map<Integer, PremixDto> premixesByNumber) {

        Status status = premixStatus.getPremixSubmissionStatus() != null
                ? premixStatus.getPremixSubmissionStatus()
                : Status.TO_BE_INITIATED;

        Map<String, Object> premixData = new LinkedHashMap<>();
        premixData.put("premixNo", premixStatus.getPremixNo());
        premixData.put("premixSubmissionStatus", status);

        if (status == Status.APPROVED) {
            PremixDto approvedPremix = premixesByNumber.get(premixStatus.getPremixNo());
            if (approvedPremix == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Approved mixing details are not available for "
                                + stageType + " premix: " + premixStatus.getPremixNo());
            }
            premixData.put("details", approvedPremix);
        }

        return premixData;
    }

    private List<Map<String, Object>> buildHardwareMotors(Batch batch) {
        String batchId = batch.getBatchId();
        String formId = casePreparationRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Case preparation form not found for batch: " + batchId))
                .getFormId();

        CasePreparationResponse formDetails =
                casePreparationService.getFormDetails(formId);

        Map<String, CasePreparationMotorRequest> motorsById = formDetails.getCasePreparationDetails() != null
                && formDetails.getCasePreparationDetails().getMotors() != null
                ? formDetails.getCasePreparationDetails().getMotors().stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .collect(Collectors.toMap(
                        CasePreparationMotorRequest::getMotorId,
                        motor -> motor,
                        (first, ignored) -> first,
                        LinkedHashMap::new))
                : Collections.emptyMap();
        String buildingNo = null;
        Map<String, HardwareProcessValues> processValuesByMotorId =
                loadHardwareProcessValues(batchId);

        return Optional.ofNullable(formDetails.getMotorStatuses())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(status -> mapHardwareMotorByApproval(
                        status, motorsById, buildingNo, processValuesByMotorId))
                .toList();
    }

    private Map<String, Object> mapHardwareMotorByApproval(
            MotorStatusResponse motorStatus,
            Map<String, CasePreparationMotorRequest> motorsById,
            String buildingNo,
            Map<String, HardwareProcessValues> processValuesByMotorId) {

        Status status = motorStatus.getMotorSubmissionStatus() != null
                ? motorStatus.getMotorSubmissionStatus()
                : Status.TO_BE_INITIATED;

        if (status != Status.APPROVED) {
            Map<String, Object> motorData = new LinkedHashMap<>();
            motorData.put("motorId", motorStatus.getMotorId());
            motorData.put("motorSubmissionStatus", status);
            return motorData;
        }

        CasePreparationMotorRequest approvedMotor = motorsById.get(motorStatus.getMotorId());
        if (approvedMotor == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Approved case preparation details are not available for motor: "
                            + motorStatus.getMotorId());
        }

        HardwareProcessValues processValues = processValuesByMotorId.get(motorStatus.getMotorId());
        String ovenNo = processValues != null ? processValues.ovenNo() : null;
        String rh = processValues != null ? processValues.rh() : null;

        QcDivisionPayloads.HardwareMotorResponse mapped = qcHardwareMapper.toHardwareMotorResponse(
                approvedMotor,
                status,
                buildingNo,
                ovenNo,
                rh);

        return objectMapper.convertValue(mapped, new TypeReference<Map<String, Object>>() {
        });
    }

    private Map<String, HardwareProcessValues> loadHardwareProcessValues(String batchId) {
        return castingCuringRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(CastingCuring::getMotors)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .map(motor -> new AbstractMap.SimpleImmutableEntry<>(
                        motor.getMotorId(),
                        new HardwareProcessValues(
                                Optional.ofNullable(motor.getCuringSetup())
                                        .map(CuringSetup::getOvenNo)
                                        .orElse(null),
                                firstCastingRh(motor))))
                .filter(entry -> entry.getValue().ovenNo() != null
                        || entry.getValue().rh() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
    }

    private String firstCastingRh(CastingCuringMotor motor) {
        return Optional.ofNullable(motor.getCastingSections())
                .map(CastingSections::getCastingProcess)
                .map(CastingProcess::getCastingFromBowlDetails)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(CastingFromBowlDetail::getRh)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .findFirst()
                .orElse(null);
    }

    private static Object findNestedValue(Object source, Set<String> normalizedKeys) {
        if (source instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (normalizedKeys.contains(normalizeDataKey(entry.getKey()))) {
                    Object value = flattenValue(entry.getValue());
                    String text = stringValue(value);
                    if (text != null && !text.isBlank()) return value;
                }
            }
            for (Object value : map.values()) {
                Object nested = findNestedValue(value, normalizedKeys);
                if (nested != null) return nested;
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                Object nested = findNestedValue(value, normalizedKeys);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static Object flattenValue(Object value) {
        if (value instanceof Collection<?> collection) {
            String flattened = collection.stream()
                    .map(QcDivisionService::stringValue)
                    .filter(Objects::nonNull)
                    .filter(item -> !item.isBlank())
                    .collect(Collectors.joining(", "));
            return flattened.isBlank() ? null : flattened;
        }
        return value;
    }

    private static String normalizeDataKey(Object key) {
        return String.valueOf(key).replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second != null && !second.isBlank() ? second : null;
    }

    private List<Map<String, Object>> buildCastingCuringMotors(
            Batch batch,
            String sectionKey) {
        String formId = castingCuringRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Casting and curing form not found for batch: " + batch.getBatchId()))
                .getFormId();

        CastingCuringFormResponse formDetails = castingCuringService.getFormDetails(formId);
        Map<String, CastingCuringMotor> motorDetailsById = Optional.ofNullable(
                        formDetails.getMotors())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .collect(Collectors.toMap(
                        CastingCuringMotor::getMotorId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        CuringCycleMaster curingConfiguration = CURING_SECTIONS.equals(sectionKey)
                && batch.getMotorStage() != null
                && batch.getProjectId() != null
                && !batch.getProjectId().isBlank()
                ? CuringCycleMasterDataService
                .findActiveByProjectIdAndMotorStage(
                        curingCycleMasterRepo,
                        batch.getProjectId().trim(),
                        batch.getMotorStage())
                .orElse(null)
                : null;
        Object subscaleCuringDetails = CURING_SECTIONS.equals(sectionKey)
                ? subscaleProcessingRepo.findByBatchIdAndIsDeletedFalse(batch.getBatchId())
                .map(this::buildQcCuringSubscaleDetails)
                .orElse(null)
                : null;

        return Optional.ofNullable(formDetails.getMotorStatuses())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(status -> mapCastingCuringMotorByApproval(
                        status,
                        motorDetailsById,
                        sectionKey,
                        batch.getMotorStage(),
                        curingConfiguration,
                        subscaleCuringDetails))
                .toList();
    }

    /**
     * QA Curing seed: manufacturing curingTable plus full hardware article selections
     * (all types — not BEM-only). STF bemMotors filtering stays separate.
     */
    private Map<String, Object> buildQcCuringSubscaleDetails(SubscaleProcessing form) {
        Map<String, Object> details = new LinkedHashMap<>();
        CuringDetailsDTO curing = form.getCuringDetails();
        details.put(
                "curingTable",
                curing != null && curing.getCuringTable() != null
                        ? curing.getCuringTable()
                        : Collections.emptyList());
        HardwarePreparationDetailsDTO hardware = form.getHardwarePreparationDetails();
        details.put(
                "hardwareArticles",
                hardware != null && hardware.getArticles() != null
                        ? hardware.getArticles()
                        : Collections.emptyList());
        return details;
    }

    private Map<String, Object> mapCastingCuringMotorByApproval(
            MotorStatusResponse motorStatus,
            Map<String, CastingCuringMotor> motorDetailsById,
            String sectionKey,
            Integer batchMotorStage,
            CuringCycleMaster curingConfiguration,
            Object subscaleCuringDetails) {
        Status status = motorStatus.getMotorSubmissionStatus() != null
                ? motorStatus.getMotorSubmissionStatus()
                : Status.TO_BE_INITIATED;

        Map<String, Object> motorData = new LinkedHashMap<>();
        motorData.put("motorId", motorStatus.getMotorId());
        motorData.put("motorSubmissionStatus", status);

        CastingCuringMotor savedMotor = motorDetailsById.get(motorStatus.getMotorId());
        if (CURING_SECTIONS.equals(sectionKey)) {
            addCuringContext(motorData, savedMotor, batchMotorStage, curingConfiguration);
        }

        if (status == Status.APPROVED) {
            if (savedMotor == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Approved casting and curing details are not available for motor: "
                                + motorStatus.getMotorId());
            }

            Map<String, Object> sectionDetails = new LinkedHashMap<>();
            Object selectedSections = CASTING_SECTIONS.equals(sectionKey)
                    ? savedMotor.getCastingSections()
                    : savedMotor.getCuringSections();
            sectionDetails.put(
                    sectionKey,
                    selectedSections != null
                            ? selectedSections
                            : Collections.emptyMap());
            if (CURING_SECTIONS.equals(sectionKey)) {
                sectionDetails.put(
                        "subscaleDetails",
                        subscaleCuringDetails != null
                                ? subscaleCuringDetails
                                : Collections.emptyMap());
            }
            motorData.put("details", sectionDetails);
        }

        return motorData;
    }

    private void addCuringContext(
            Map<String, Object> motorData,
            CastingCuringMotor savedMotor,
            Integer batchMotorStage,
            CuringCycleMaster curingConfiguration) {
        String configuredStage = curingConfiguration != null
                ? curingConfiguration.getMotorStageName()
                : null;
        String motorStage = firstNonBlank(configuredStage,
                batchMotorStage == null ? null : "Stage " + batchMotorStage);

        String configuredCuringType = curingConfiguration != null
                ? curingConfiguration.getCuringType()
                : null;
        CuringSetup curingSetup = savedMotor != null ? savedMotor.getCuringSetup() : null;
        String curingType = firstNonBlank(
                curingSetup != null ? curingSetup.getCuringType() : null,
                configuredCuringType);

        String oven = curingSetup != null ? curingSetup.getOven() : null;
        String ovenNumber = curingSetup != null ? curingSetup.getOvenNo() : null;

        motorData.put("motorStage", motorStage);
        motorData.put("curingType", curingType);
        motorData.put("oven", oven);
        motorData.put("ovenNumber", ovenNumber);
    }

    private List<Map<String, Object>> buildDeCoringMotors(String batchId) {
        String formId = castingCuringRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Casting and curing form not found for batch: " + batchId))
                .getFormId();

        CastingCuringFormResponse formDetails = castingCuringService.getFormDetails(formId);
        Map<String, CastingCuringMotor> motorDetailsById = Optional.ofNullable(
                        formDetails.getMotors())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .collect(Collectors.toMap(
                        CastingCuringMotor::getMotorId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        return Optional.ofNullable(formDetails.getMotorStatuses())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(status -> mapDeCoringMotorByApproval(status, motorDetailsById))
                .toList();
    }

    private Map<String, Object> mapDeCoringMotorByApproval(
            MotorStatusResponse motorStatus,
            Map<String, CastingCuringMotor> motorDetailsById) {
        Status status = motorStatus.getMotorSubmissionStatus() != null
                ? motorStatus.getMotorSubmissionStatus()
                : Status.TO_BE_INITIATED;

        Map<String, Object> motorData = new LinkedHashMap<>();
        motorData.put("motorId", motorStatus.getMotorId());
        motorData.put("motorSubmissionStatus", status);

        if (status == Status.APPROVED) {
            CastingCuringMotor savedMotor = motorDetailsById.get(motorStatus.getMotorId());
            if (savedMotor == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Approved casting and curing details are not available for motor: "
                                + motorStatus.getMotorId());
            }
            DecoringDetails decoringDetails = savedMotor.getCuringSections() != null
                    ? savedMotor.getCuringSections().getDecoringDetails()
                    : null;
            motorData.put("details", Map.of(
                    "decoringDetails",
                    decoringDetails != null ? decoringDetails : Collections.emptyMap()));
        }

        return motorData;
    }

    private static Object findSectionData(Object source, String normalizedSectionId) {
        if (source instanceof Map<?, ?> map) {
            if (normalizedSectionId.equals(normalizeDataKey(map.get("sectionId")))) {
                return map.get("sectionData");
            }
            for (Object value : map.values()) {
                Object sectionData = findSectionData(value, normalizedSectionId);
                if (sectionData != null) return sectionData;
            }
        } else if (source instanceof Collection<?> collection) {
            for (Object value : collection) {
                Object sectionData = findSectionData(value, normalizedSectionId);
                if (sectionData != null) return sectionData;
            }
        }
        return null;
    }

    private List<Map<String, Object>> buildTrimmingMotors(String batchId) {
        String formId = trimmingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Trimming form not found for batch: " + batchId))
                .getFormId();

        TrimmingDetailsResponse formDetails = trimmingService.getFormDetails(formId);

        Map<String, Map<String, Object>> motorDetailsById = Optional.ofNullable(formDetails.getMotors())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .collect(Collectors.toMap(
                        motor -> String.valueOf(motor.getMotorId()),
                        motor -> objectMapper.convertValue(motor, new TypeReference<Map<String, Object>>() {
                        }), // 👈 Converts MotorDTO to Map<String, Object>
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        return Optional.ofNullable(formDetails.getMotorStatuses())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(status -> mapTrimmingMotorByApproval(status, motorDetailsById))
                .toList();
    }


    private Map<String, Object> mapTrimmingMotorByApproval(
            MotorStatusResponse motorStatus,
            Map<String, Map<String, Object>> motorDetailsById) {
        Status status = motorStatus.getMotorSubmissionStatus() != null
                ? motorStatus.getMotorSubmissionStatus()
                : Status.TO_BE_INITIATED;

        Map<String, Object> motorData = new LinkedHashMap<>();
        motorData.put("motorId", motorStatus.getMotorId());
        motorData.put("motorSubmissionStatus", status);

        if (status == Status.APPROVED) {
            Map<String, Object> savedMotor = motorDetailsById.get(motorStatus.getMotorId());
            if (savedMotor == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Approved trimming details are not available for motor: "
                                + motorStatus.getMotorId());
            }
            motorData.put("details", mapTrimmingDetails(
                    savedMotor, motorStatus.getMotorId()));
        }
        return motorData;
    }

    private Map<String, Object> mapTrimmingDetails(
            Map<String, Object> savedMotor,
            String motorId) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("motorIdNo", motorId);
        details.put("motorStage", savedMotor.get("motorStage"));
        details.put("motorReceivedDateTime", firstNonNull(
                firstNonNull(
                        savedMotor.get("motorReceivedDateTime"),
                        savedMotor.get("motorReceivedDate")),
                savedMotor.get("motorReceivedAt")));
        details.put("trimmingOperationDetails", trimmingOperationDetails(savedMotor));
        details.put("trimmingMeasurementDetails", trimmingMeasurementDetails(savedMotor));
        details.put("measurementQaRemarks", savedMotor.get("measurementQaRemarks"));
        details.put("trimmingRemarks", trimmingRemarks(savedMotor));
        return details;
    }

    private List<Map<String, Object>> trimmingOperationDetails(
            Map<String, Object> savedMotor) {
        Object direct = savedMotor.get("trimmingOperationDetails");
        Object source = direct != null
                ? direct : findSectionData(savedMotor, "TRIMMINGDETAILS");
        return optionalCopiedMapList(source, "trimmingOperationDetails");
    }

    private List<Map<String, Object>> trimmingMeasurementDetails(
            Map<String, Object> savedMotor) {
        Object direct = savedMotor.get("trimmingMeasurementDetails");
        if (direct != null) {
            return optionalCopiedMapList(direct, "trimmingMeasurementDetails");
        }

        List<Map<String, Object>> parameters = optionalCopiedMapList(
                findSectionData(savedMotor, "TRIMMINGCOMMONFORMAT"),
                "TRIMMING_COMMON_FORMAT.sectionData");
        List<Map<String, Object>> measurements = new ArrayList<>();
        int serialNumber = 1;
        for (Map<String, Object> parameter : parameters) {
            Object stagesValue = parameter.get("stages");
            if (!(stagesValue instanceof Collection<?> stages)) continue;
            for (Object stageValue : stages) {
                if (!(stageValue instanceof Map<?, ?> stageSource)) continue;
                Map<String, Object> stage = new LinkedHashMap<>();
                stageSource.forEach((key, value) -> stage.put(String.valueOf(key), value));
                Map<String, Object> readings = stage.get("readings") instanceof Map<?, ?> source
                        ? mapCopy(source) : Collections.emptyMap();

                Map<String, Object> measurement = new LinkedHashMap<>();
                measurement.put("SR_NO", serialNumber++);
                measurement.put("DIMENSION", firstNonNull(
                        parameter.get("parameterName"), parameter.get("parameterId")));
                measurement.put("SPECIFIED", stage.get("specification"));
                measurement.put("MEASUREMENT_STAGE", stage.get("stageName"));
                for (String reading : List.of("R2T", "R2B", "R1L", "R1R")) {
                    measurement.put(reading, readings.get(reading));
                }
                measurements.add(measurement);
            }
        }
        return measurements;
    }

    private Map<String, Object> trimmingRemarks(Map<String, Object> savedMotor) {
        Object direct = savedMotor.get("trimmingRemarks");
        if (direct instanceof Map<?, ?> map) return mapCopy(map);

        List<Map<String, Object>> remarks = optionalCopiedMapList(
                findSectionData(savedMotor, "TRIMMINGREMARKS"),
                "TRIMMING_REMARKS.sectionData");
        Map<String, Object> source = remarks.isEmpty()
                ? Collections.emptyMap() : remarks.getFirst();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("remarks", source.get("remarks"));
        if (source.get("qaRemarks") != null) {
            result.put("qaRemarks", source.get("qaRemarks"));
        }
        // Manufacturing stores uploads as reportFile (List<DocumentDto>);
        // QC TrimmingRemarks DTO / FE hydrate expect the same key.
        Object reportFile = source.get("reportFile");
        if (reportFile == null) {
            reportFile = source.get("reportFiles");
        }
        if (reportFile != null) {
            result.put("reportFile", reportFile);
        } else if (source.get("reportLink") != null) {
            // Legacy string/URL-only uploads.
            result.put("reportLink", source.get("reportLink"));
        }
        return result;
    }

    private List<Map<String, Object>> buildPostCureMotors(String batchId) {
        String formId = postCureRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Post Cure form not found for batch: " + batchId))
                .getFormId();

        PostCureResponse formDetails = postCureOptService.getFormDetails(formId);
        Map<String, PostCureMotor> motorDetailsById = Optional.ofNullable(
                        formDetails.getMotors())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .collect(Collectors.toMap(
                        PostCureMotor::getMotorId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        return Optional.ofNullable(formDetails.getMotorStatuses())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(status -> mapPostCureMotorByApproval(status, motorDetailsById))
                .toList();
    }

    private Map<String, Object> mapPostCureMotorByApproval(
            MotorStatusResponse motorStatus,
            Map<String, PostCureMotor> motorDetailsById) {
        Status status = motorStatus.getMotorSubmissionStatus() != null
                ? motorStatus.getMotorSubmissionStatus()
                : Status.TO_BE_INITIATED;

        Map<String, Object> motorData = new LinkedHashMap<>();
        motorData.put("motorId", motorStatus.getMotorId());
        motorData.put("motorSubmissionStatus", status);

        if (status == Status.APPROVED) {
            PostCureMotor approvedMotor = motorDetailsById.get(motorStatus.getMotorId());
            if (approvedMotor == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Approved post cure details are not available for motor: "
                                + motorStatus.getMotorId());
            }
            // Flatten dual sections for QC create/update/details round-trip.
            Map<String, Object> flat = objectMapper.convertValue(
                    approvedMotor,
                    new TypeReference<Map<String, Object>>() {
                    });
            motorData.putAll(flat);
            motorData.put("details", approvedMotor);
            motorData.put("motorId", motorStatus.getMotorId());
            motorData.put("motorSubmissionStatus", status);
        }
        return motorData;
    }

    private List<Map<String, Object>> buildNdtMotors(String batchId) {
        String formId = ndtRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "NDT form not found for batch: " + batchId))
                .getFormId();

        NdtResponse formDetails = ndtService.getFormDetails(formId);
        List<Map<String, Object>> motors = buildApprovedMotorDetails(
                formDetails.getMotorStatuses(),
                formDetails.getMotors()
        );
        enrichNdtAutoPopulateMotors(motors);
        return motors;
    }

    /**
     * Seed QC STF division from the STF subdepartment form ({@code static_testings}).
     * Prefer approved motors; when none are approved yet, return all motors for editing.
     */
    private List<Map<String, Object>> buildStaticTestFacilityMotors(String batchId) {
        String formId = staticTestingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.FORM_NOT_FOUND,
                        "Static Test Facility form not found for batch: " + batchId))
                .getFormId();

        StfResponse formDetails = staticTestingService.getFormDetails(formId);
        List<StaticMotorData> sourceMotors = Optional.ofNullable(formDetails.getMotors())
                .orElseGet(Collections::emptyList);
        List<Map<String, Object>> motors = new ArrayList<>();
        for (StaticMotorData motor : sourceMotors) {
            if (motor == null || motor.getMotorId() == null || motor.getMotorId().isBlank()) {
                continue;
            }
            Map<String, Object> row = objectMapper.convertValue(
                    motor,
                    new TypeReference<Map<String, Object>>() {
                    });
            motors.add(row);
        }
        return motors;
    }

    private void enrichNdtAutoPopulateMotors(List<Map<String, Object>> motors) {
        if (motors == null || motors.isEmpty()) {
            return;
        }
        for (Map<String, Object> motorData : motors) {
            if (motorData == null) {
                continue;
            }
            Object detailsNode = motorData.get("details");
            if (!(detailsNode instanceof Map<?, ?>)) {
                continue;
            }
            NdtMotorDto manufacturingMotor = objectMapper.convertValue(detailsNode, NdtMotorDto.class);
            overlayRadiographyPlanFromRmc(manufacturingMotor);
            QcDivisionPayloads.NdtMotorInput qcMotor = qcNdtMapper.toQcMotor(manufacturingMotor);
            enrichQcNdtMotorDocuments(qcMotor);

            Object submissionStatus = motorData.get("motorSubmissionStatus");
            Map<String, Object> flat = objectMapper.convertValue(
                    qcMotor,
                    new TypeReference<Map<String, Object>>() {
                    });
            motorData.clear();
            motorData.putAll(flat);
            if (submissionStatus != null) {
                motorData.put("motorSubmissionStatus", submissionStatus);
            }
        }
    }

    private void enrichQcNdtMotorDocuments(
            QcDivisionPayloads.NdtMotorInput motor) {
        if (motor == null) {
            return;
        }
        String motorId = motor.getMotorId();
        if (motor.getVisualInspectionDetails() != null) {
            for (QcDivisionPayloads.QcNdtVisualInspectionDetails row : motor.getVisualInspectionDetails()) {
                if (row != null) {
                    enrichAttachmentList(row.getUploadedImages(), 10L, motorId);
                }
            }
        }
        if (motor.getRadiographyObservations() != null) {
            for (QcDivisionPayloads.QcNdtRadiographyObservation row : motor.getRadiographyObservations()) {
                if (row != null) {
                    enrichAttachmentList(row.getUploadedImages(), 10L, motorId);
                }
            }
        }
        enrichAttachmentList(motor.getUploadedVideos(), 10L, motorId);
        if (motor.getSignedNdtReport() != null) {
            enrichDocumentMetadata(motor.getSignedNdtReport().getReport(), 10L, motorId);
        }
    }

    private void enrichQcNdtMotorDocuments(
            QcDivisionPayloads.NdtMotorResponse motor,
            Long subDepartmentId) {
        if (motor == null) {
            return;
        }
        String motorId = motor.getMotorId();
        if (motor.getVisualInspectionDetails() != null) {
            for (QcDivisionPayloads.QcNdtVisualInspectionDetails row : motor.getVisualInspectionDetails()) {
                if (row != null) {
                    enrichAttachmentList(row.getUploadedImages(), subDepartmentId, motorId);
                }
            }
        }
        if (motor.getRadiographyObservations() != null) {
            for (QcDivisionPayloads.QcNdtRadiographyObservation row : motor.getRadiographyObservations()) {
                if (row != null) {
                    enrichAttachmentList(row.getUploadedImages(), subDepartmentId, motorId);
                }
            }
        }
        enrichAttachmentList(motor.getUploadedVideos(), subDepartmentId, motorId);
        if (motor.getSignedNdtReport() != null) {
            enrichDocumentMetadata(motor.getSignedNdtReport().getReport(), subDepartmentId, motorId);
        }
    }

    private void overlayRadiographyPlanFromRmc(NdtMotorDto motor) {
        if (motor == null || motor.getMotorId() == null || motor.getMotorId().isBlank()) {
            return;
        }
        RadiographyDetailsDto existing = motor.getRadiographyDetails();
        boolean hasPlanId = existing != null
                && existing.getRadiographyPlanId() != null
                && !existing.getRadiographyPlanId().isBlank();
        boolean hasPlanDetails = existing != null
                && existing.getRadiographyPlanDetails() != null
                && !existing.getRadiographyPlanDetails().isEmpty();
        if (hasPlanId && hasPlanDetails) {
            return;
        }

        RadiographyDetails rmcRadiography = findRmcRadiographyDetails(motor.getMotorId());
        if (rmcRadiography == null) {
            return;
        }

        RadiographyDetailsDto target = existing != null ? existing : new RadiographyDetailsDto();
        if (!hasPlanId
                && rmcRadiography.getRadiographyPlanId() != null
                && !rmcRadiography.getRadiographyPlanId().isBlank()) {
            target.setRadiographyPlanId(rmcRadiography.getRadiographyPlanId());
        }
        if (!hasPlanDetails && rmcRadiography.getRadiographyPlanDetails() != null) {
            target.setRadiographyPlanDetails(
                    rmcRadiography.getRadiographyPlanDetails());
        }
        motor.setRadiographyDetails(target);
    }

    private RadiographyDetails findRmcRadiographyDetails(String motorId) {
        Optional<RocketMotorCasing> byCasingId = rmcRepository.findByMotorCasingId(motorId);
        RocketMotorCasing casing = byCasingId.orElseGet(() -> {
            List<RocketMotorCasing> byMotorId =
                    rmcRepository.findByMotorIdInAndActiveTrue(List.of(motorId));
            return byMotorId.isEmpty() ? null : byMotorId.getFirst();
        });
        if (casing == null) {
            return null;
        }
        Sections sections = casing.getSections();
        return sections == null ? null : sections.getRadiographyDetails();
    }



    private List<Map<String, Object>> buildApprovedMotorDetails(
            List<MotorStatusResponse> motorStatuses,
            List<NdtMotorDto> savedMotors) {
        Map<String, Map<String, Object>> motorDetailsById = Optional.ofNullable(savedMotors)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null)
                .collect(Collectors.toMap(
                        motor -> motor.getMotorId().trim(),
                        this::toMotorMap,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        return Optional.ofNullable(motorStatuses)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(status -> mapMotorDetailsByApproval(
                        status,
                        motorDetailsById
                ))
                .toList();
    }
    private Map<String, Object> toMotorMap(NdtMotorDto motor) {
        if (motor == null) {
            return Collections.emptyMap();
        }

        return objectMapper.convertValue(
                motor,
                new TypeReference<Map<String, Object>>() {}
        );
    }
    private Map<String, Object> mapMotorDetailsByApproval(
            MotorStatusResponse motorStatus,
            Map<String, Map<String, Object>> motorDetailsById) {
        Status status = motorStatus.getMotorSubmissionStatus() != null
                ? motorStatus.getMotorSubmissionStatus()
                : Status.TO_BE_INITIATED;

        Map<String, Object> motorData = new LinkedHashMap<>();
        motorData.put("motorId", motorStatus.getMotorId());
        motorData.put("motorSubmissionStatus", status);

        if (status == Status.APPROVED) {
            Map<String, Object> approvedMotor = motorDetailsById.get(motorStatus.getMotorId());
            if (approvedMotor == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Approved " + "NDT" + " details are not available for motor: "
                                + motorStatus.getMotorId());
            }
            motorData.put("details", approvedMotor);
        }

        return motorData;
    }

    private ResolvedDivision resolveDivision(Long divisionId) {
        QcDivisionMaster master = qcDivisionMasterRepo.findByDivisionOrTypeId(divisionId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "QC division not found: " + divisionId));

        if (Objects.equals(master.getDivisionId(), divisionId)) {
            return new ResolvedDivision(
                    master.getDivisionId(),
                    master.getDivisionName(),
                    Boolean.TRUE.equals(master.getDisabled()));
        }

        return Optional.ofNullable(master.getTypes())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(type -> Objects.equals(type.getDivisionId(), divisionId))
                .findFirst()
                .map(type -> new ResolvedDivision(
                        type.getDivisionId(),
                        type.getDivisionName(),
                        Boolean.TRUE.equals(master.getDisabled())
                                || Boolean.TRUE.equals(type.getDisabled())))
                .orElseThrow(() -> new AcemException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "QC division not found: " + divisionId));
    }


    private Map<String, Object> toApRevalidationLot(
            MaterialMaster master,
            int srNo,
            String lotId,
            List<Map<String, Object>> specifications) {

        // Copy per lot — shared master specs must not accumulate another lot's results.
        List<Map<String, Object>> lotSpecifications = copyRevalidationSpecifications(specifications);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("srNo", srNo);
        data.put("materialCode", master.getMaterialCode());
        data.put("materialName", master.getMaterialName());
        data.put("lotId", lotId);
        data.put("specifications", lotSpecifications);

        try {
            LotDetailsResponse lotDetails = rawMaterialSourcingService.getLotDetails(
                    LotDetailsRequest.builder()
                            .lotId(lotId)
                            .build());
            data.put("submissionType", lotDetails.getSubmissionType());
            data.put("subDepartmentId", lotDetails.getSubDepartmentId());
            data.put("supplyOrderNo", lotDetails.getSupplyOrderNo());
            data.put("receiptDate", lotDetails.getReceiptDate());
            data.put("manufacturerName", lotDetails.getManufacturerName());
            data.put("certificates", lotDetails.getCertificates() != null
                    ? lotDetails.getCertificates()
                    : Collections.emptyList());
            // Analysed Result on the revalidation form is sourced from the lot (read-only in UI).
            mergeLotAnalysedResultsIntoSpecs(lotSpecifications, lotDetails.getSpecifications());
        } catch (AcemException ex) {
            if (ex.getErrorCode().getStatus() != HttpStatus.NOT_FOUND) {
                throw ex;
            }
            log.warn(
                    "Lot {} is not available for AP revalidation on material {}: {}",
                    lotId,
                    master.getMaterialCode(),
                    ex.getMessage());
            data.put("certificates", Collections.emptyList());
        }
        return data;
    }

    private List<Map<String, Object>> copyRevalidationSpecifications(
            List<Map<String, Object>> specifications) {
        if (specifications == null || specifications.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> copy = new ArrayList<>(specifications.size());
        for (Map<String, Object> row : specifications) {
            copy.add(row == null ? new LinkedHashMap<>() : new LinkedHashMap<>(row));
        }
        return copy;
    }

    /**
     * Copy lot sourcing analysed results onto master-driven revalidation spec rows.
     * Match by specificationCode first, then specificationName.
     */
    private void mergeLotAnalysedResultsIntoSpecs(
            List<Map<String, Object>> specifications,
            List<SpecificationResponse> lotSpecs) {
        if (specifications == null || specifications.isEmpty()
                || lotSpecs == null || lotSpecs.isEmpty()) {
            return;
        }

        Map<String, SpecificationResponse> byCode = new LinkedHashMap<>();
        Map<String, SpecificationResponse> byName = new LinkedHashMap<>();
        for (SpecificationResponse lotSpec : lotSpecs) {
            if (lotSpec == null) {
                continue;
            }
            String code = trimmedText(lotSpec.getSpecificationCode());
            String name = trimmedText(lotSpec.getSpecificationName());
            if (code != null) {
                byCode.putIfAbsent(code, lotSpec);
            }
            if (name != null) {
                byName.putIfAbsent(name, lotSpec);
            }
        }

        for (Map<String, Object> row : specifications) {
            if (row == null) {
                continue;
            }
            String code = trimmedText(stringValue(row.get("specificationCode")));
            String name = trimmedText(stringValue(row.get("specificationName")));
            SpecificationResponse match = code != null ? byCode.get(code) : null;
            if (match == null && name != null) {
                match = byName.get(name);
            }
            if (match == null) {
                continue;
            }
            String analysedResult = trimmedText(match.getAnalysedResult());
            if (analysedResult == null) {
                continue;
            }
            row.put("analysedResult", analysedResult);
            row.put("result", analysedResult);
        }
    }

    private static String trimmedText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ResolvedDivision(
            Long divisionId,
            String divisionName,
            boolean disabled) {
    }

    public QcDivisionFormResponse createForm(CreateQcDivisionRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        Batch batch = requireBatch(request.getBatchId());

        if (qcDivisionRepo.existsByBatchIdAndIsDeletedFalse(request.getBatchId())) {
            String existingFormId = qcDivisionRepo.findByBatchIdAndIsDeletedFalse(request.getBatchId())
                    .map(QcDivision::getFormId).orElse(null);
            throw new AcemException(
                    ErrorCode.QC_FORM_ALREADY_EXISTS,
                    "QC division form already exists",
                    "Use update API. Form " + existingFormId + " already exists for this batch");
        }

        batchService.assertStageUnlockedAndActive(batch, request.getSubDepartmentId(), "QC Division");
        if (request.getFormSubmissionType() != Status.DRAFT) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "A QC division form must be created as DRAFT");
        }

        PersonInfo actor = personInfo(userId);
        LocalDateTime now = LocalDateTime.now();
        List<QcDivisionWorkflow> workflow = buildWorkflow(batch, actor, now);
        if (workflow.isEmpty()) {
            throw new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "Enabled QC division master data not found");
        }

        QcDivision form = QcDivision.builder()
                .formId(formIdGenerator.generate("qc_divisions", "formId", "QC-F", 4))
                .batchId(request.getBatchId())
                .subDepartmentId(request.getSubDepartmentId())
                .formSubmissionType(Status.DRAFT)
                .status(Status.IN_PROGRESS)
                .divisionDetails(new ArrayList<>())
                .divisionWorkflow(workflow)
                .createdBy(actor)
                .createdAt(now)
                .build();
        // Seed locks are static; manufacturing approvals may already have unlocked divisions/units.
        syncWorkflowLocksFromStageProgress(form, batch, actor, now);

        validateRevalidationAcemQcResults(request.getDivisionDetails());

        QcFileProcessResult fileProcessResult = new QcFileProcessResult(List.of());

        try {
            if (request.getDivisionDetails() != null && !request.getDivisionDetails().isEmpty()) {
                fileProcessResult = processQcFilesAndCleanup(
                        request.getDivisionDetails(),
                        request.getSubDepartmentId(),
                        form.getFormId(),
                        null);
            }

            ActionContext action = mergeDivisionPayloads(
                    form, batch, qcDivisionPayloadMapper.toMaps(request.getDivisionDetails()), actor, now);
            Aggregation aggregation = aggregate(form, batch);
            applyAggregateStatus(form, aggregation);

            form = qcDivisionRepo.save(form);
            syncProgress(form, aggregation, actor, now, false);
            log.info("QC Division form {} created by {}", form.getFormId(), userId);

            return buildResponse(form, batch, aggregation, action, false);

        } catch (Exception ex) {
            log.error("QC Division form create failed for formId={}. Rolling back newly promoted files.",
                    form.getFormId(), ex);
            for (DocumentDto doc : fileProcessResult.promotedTemps()) {
                if (doc != null && doc.getFileId() != null) {
                    fileBindingService.rollbackOrUnbindFile(doc.getFileId());
                }
            }
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE FORM
    // -------------------------------------------------------------------------
    public QcDivisionFormResponse updateForm(UpdateQcDivisionRequest request, String userId) {
        ApprovalStatusUtil.requireDraftFormWorkflow(request.getFormSubmissionType());
        QcDivision form = requireForm(request.getFormId());
        validateFormIdentity(form, request.getBatchId(), request.getSubDepartmentId());
        Batch batch = requireActiveBatch(request.getBatchId(), request.getSubDepartmentId());
        ensureWorkflow(form, personInfo(userId), LocalDateTime.now());

        if (form.getStatus() == Status.COMPLETELY_APPROVED) {
            throw new AcemException(ErrorCode.UNIT_NOT_EDITABLE, "Submitted QC division forms cannot be modified");
        }

        PersonInfo actor = personInfo(userId);
        LocalDateTime now = LocalDateTime.now();
        boolean finalSubmit = request.getFormSubmissionType() == Status.SUBMIT;
        ActionContext action = null;

        if (finalSubmit) {
            if (request.getDivisionDetails() != null && !request.getDivisionDetails().isEmpty()) {
                throw new AcemException(ErrorCode.INVALID_REQUEST, "divisionDetails must be empty for final QC submission");
            }
            Aggregation ready = aggregate(form, batch);
            if (!ready.allDivisionsApproved()) {
                throw new AcemException(ErrorCode.FINAL_APPROVAL_NOT_READY, "Final approval not ready", buildPendingDivisionDetails(ready));
            }
            form.setFormSubmissionType(Status.SUBMIT);
            form.setStatus(Status.COMPLETELY_APPROVED);
            form.setSubmittedBy(actor);
            form.setSubmittedOn(now);
        } else {
            form.setFormSubmissionType(Status.DRAFT);
        }

        form.setUpdatedBy(actor);
        form.setUpdatedAt(now);

        if (!finalSubmit) {
            validateRevalidationAcemQcResults(request.getDivisionDetails());
        }

        QcFileProcessResult fileProcessResult = new QcFileProcessResult(List.of());

        try {
            if (!finalSubmit && request.getDivisionDetails() != null && !request.getDivisionDetails().isEmpty()) {
                // Promotes QC temp uploads, enriches referenced fileIds, purges removed QC-owned files
                fileProcessResult = processQcFilesAndCleanup(
                        request.getDivisionDetails(),
                        request.getSubDepartmentId(),
                        request.getFormId(),
                        form);
            }

            if (!finalSubmit) {
                action = mergeDivisionPayloads(form, batch, qcDivisionPayloadMapper.toMaps(request.getDivisionDetails()), actor, now);
            }

            Aggregation aggregation = aggregate(form, batch);
            if (!finalSubmit) {
                applyAggregateStatus(form, aggregation);
            }

            form = qcDivisionRepo.save(form);
            syncProgress(form, aggregation, actor, now, finalSubmit);
            log.info("QC Division form {} updated by {}", form.getFormId(), userId);

            return buildResponse(form, batch, aggregation, action, true);

        } catch (Exception ex) {
            log.error("QC Division form update failed for formId={}. Rolling back newly promoted files.", request.getFormId(), ex);
            for (DocumentDto doc : fileProcessResult.promotedTemps()) {
                if (doc != null && doc.getFileId() != null) {
                    fileBindingService.rollbackOrUnbindFile(doc.getFileId());
                }
            }
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // FILE MANAGEMENT HELPERS
    // -------------------------------------------------------------------------
    private record QcFileProcessResult(List<DocumentDto> promotedTemps) {
    }

    private QcFileProcessResult processQcFilesAndCleanup(
            List<QcDivisionPayloads.DivisionDetailInput> divisionDetails,
            Long subDeptId,
            String formId,
            QcDivision persistedForm) {

        if (divisionDetails == null || divisionDetails.isEmpty()) {
            return new QcFileProcessResult(List.of());
        }

        // 1. Purge QC-owned files removed only within divisions/units included in this request
        List<String> removedFileIds = computeScopedRemovedFileIds(persistedForm, divisionDetails);
        if (!removedFileIds.isEmpty()) {
            fileBindingService.purgeRemovedFilesFromDiskAndDb(formId, subDeptId, removedFileIds);
        }

        // 2. Fetch existing attachments bound specifically to this QC Form
        List<FileAttachments> existingAttachments = fileBindingService.getAttachmentsByFormId(formId);
        Set<String> existingFormFileIds = existingAttachments.stream()
                .map(FileAttachments::getFileId)
                .collect(Collectors.toSet());

        // 3. Recursive traversal: promote QC temps, enrich existing referenced fileIds
        List<DocumentDto> newlyPromotedDocs = new ArrayList<>();
        for (QcDivisionPayloads.DivisionDetailInput detail : divisionDetails) {
            if (detail == null || detail.getData() == null) {
                continue;
            }
            JsonNode rootNode = objectMapper.valueToTree(detail.getData());
            // Detach weightmentSheet before typed convertValue — that round-trip was wiping it to null.
            JsonNode weightmentSnapshot = null;
            if (rootNode instanceof ObjectNode objectNode && objectNode.has("weightmentSheet")) {
                weightmentSnapshot = objectNode.remove("weightmentSheet");
            }
            QcFileScopeContext scopeContext = new QcFileScopeContext(
                    null,
                    resolvePayloadDivision(detail.getDivision(), detail.getSubType()));
            promoteAndEnrichNode(
                    rootNode,
                    subDeptId,
                    formId,
                    existingFormFileIds,
                    newlyPromotedDocs,
                    scopeContext);

            QcDivisionPayloads.DivisionDataInput converted = objectMapper.convertValue(
                    rootNode, objectMapper.constructType(detail.getData().getClass()));
            if (converted instanceof QcDivisionPayloads.RawMaterialProcessingDataInput processing
                    && weightmentSnapshot != null
                    && !weightmentSnapshot.isNull()) {
                processing.setWeightmentSheet(objectMapper.convertValue(
                        weightmentSnapshot,
                        in.co.bel.acempp.dto.rawmaterialpreparation.WeightmentSheetRequest.class));
            }
            detail.setData(converted);
        }

        return new QcFileProcessResult(newlyPromotedDocs);
    }

    private void promoteAndEnrichNode(
            JsonNode node,
            Long subDeptId,
            String formId,
            Set<String> existingFormFileIds,
            List<DocumentDto> newlyPromotedDocs,
            QcFileScopeContext scopeContext) {

        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            QcFileScopeContext nodeContext = scopeContext.withNode(objectNode);

            if (objectNode.hasNonNull("fileId")) {
                String fileId = objectNode.get("fileId").asText().trim();

                if (!fileId.isEmpty()) {
                    if (existingFormFileIds != null && existingFormFileIds.contains(fileId)) {
                        enrichQcFileNode(objectNode, nodeContext);
                    } else {
                        Optional<FileAttachments> existingAttachment = fileAttachmentLookupService.resolve(
                                fileId, null, nodeContext.motorId());
                        if (existingAttachment.isPresent()
                                && !isQcTempUpload(existingAttachment.get(), subDeptId)) {
                            enrichQcReferencedFileNode(objectNode, nodeContext);
                        } else {
                            applyQcFileBinding(
                                    objectNode,
                                    fileId,
                                    subDeptId,
                                    formId,
                                    newlyPromotedDocs,
                                    nodeContext.resolveBindingScope());
                        }
                    }
                }
            }

            objectNode.fields().forEachRemaining(entry ->
                    promoteAndEnrichNode(
                            entry.getValue(),
                            subDeptId,
                            formId,
                            existingFormFileIds,
                            newlyPromotedDocs,
                            nodeContext)
            );

        } else if (node.isArray()) {
            for (JsonNode element : node) {
                promoteAndEnrichNode(
                        element,
                        subDeptId,
                        formId,
                        existingFormFileIds,
                        newlyPromotedDocs,
                        scopeContext);
            }
        }
    }

    private boolean isQcTempUpload(FileAttachments attachment, Long qcSubDeptId) {
        if (attachment == null || !Objects.equals(attachment.getSubDepartmentId(), qcSubDeptId)) {
            return false;
        }
        String status = attachment.getStatus();
        return "TEMP".equals(status) || "TEMP_SUCCESS".equals(status);
    }

    private void enrichQcFileNode(ObjectNode objectNode, QcFileScopeContext scopeContext) {
        fileMetadataEnrichmentService.enrichJsonFileNodes(
                objectNode,
                scopeContext.sourceSubDepartmentId(),
                scopeContext.motorId());
        fileAttachmentLookupService
                .resolve(objectNode.get("fileId").asText().trim(),
                        scopeContext.sourceSubDepartmentId(),
                        scopeContext.motorId())
                .ifPresent(attachment -> applyAttachmentMetadata(objectNode, attachment));
    }

    private void enrichQcReferencedFileNode(ObjectNode objectNode, QcFileScopeContext scopeContext) {
        enrichQcFileNode(objectNode, scopeContext);
        objectNode.put("readOnly", true);
    }

    private void applyQcFileBinding(
            ObjectNode objectNode,
            String fileId,
            Long subDeptId,
            String formId,
            List<DocumentDto> newlyPromotedDocs,
            FileBindingScope scope) {
        try {
            FileAttachments promoted = fileBindingService.promoteAndBindFile(
                    fileId, subDeptId, formId, scope);
            applyAttachmentMetadata(objectNode, promoted);
            newlyPromotedDocs.add(toDocumentDto(promoted));
        } catch (AcemException promoteEx) {
            if (promoteEx.getErrorCode() == ErrorCode.FILE_NOT_FOUND) {
                log.error("File not found during QC tree traversal for fileId: {} bound to formId: {}",
                        fileId, formId, promoteEx);
                throw new AcemException(
                        ErrorCode.FILE_NOT_FOUND,
                        "Referenced file not found or is not yet available: " + fileId
                );
            }
            log.error("File promotion failed during QC tree traversal for fileId: {} bound to formId: {}",
                    fileId, formId, promoteEx);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "Unable to process one or more attachments. Please re-upload the file and try again."
            );
        } catch (Exception e) {
            log.error("File promotion failed during QC tree traversal for fileId: {} bound to formId: {}",
                    fileId, formId, e);
            throw new AcemException(
                    ErrorCode.FILE_TRANSFER_ERROR,
                    "Unable to process one or more attachments. Please re-upload the file and try again."
            );
        }
    }

    private void applyAttachmentMetadata(ObjectNode objectNode, FileAttachments attachment) {
        if (attachment == null) {
            return;
        }
        objectNode.put("fileName", attachment.getOriginalFileName());
        objectNode.put("fileUrl", attachment.getFilePath());
        objectNode.put("status", attachment.getStatus());
        objectNode.put("mimeType", attachment.getMimeType());
        objectNode.put("readOnly", attachment.isReadOnly());
    }

    private DocumentDto toDocumentDto(FileAttachments attachment) {
        return DocumentDto.builder()
                .fileId(attachment.getFileId())
                .fileName(attachment.getOriginalFileName())
                .fileUrl(attachment.getFilePath())
                .status(attachment.getStatus())
                .mimeType(attachment.getMimeType())
                .build();
    }

    private List<String> computeScopedRemovedFileIds(
            QcDivision persistedForm,
            List<QcDivisionPayloads.DivisionDetailInput> incoming) {
        if (persistedForm == null || incoming == null || incoming.isEmpty()) {
            return List.of();
        }

        Set<String> removed = new LinkedHashSet<>();
        for (QcDivisionPayloads.DivisionDetailInput detail : incoming) {
            if (detail == null || detail.getData() == null) {
                continue;
            }
            String canonicalDivision = resolvePayloadDivision(detail.getDivision(), detail.getSubType());
            String subType = detail.getSubType();

            if (DIVISION_SCOPED_DIVISIONS.contains(canonicalDivision)) {
                Object previousData = findDetail(persistedForm, canonicalDivision, subType)
                        .map(DivisionDetail::getData)
                        .orElse(null);
                collectRemovedFileIds(removed, previousData, detail.getData());
            } else if (PREMIX_DIVISIONS.contains(canonicalDivision)) {
                collectRemovedPremixUnitFileIds(
                        removed, persistedForm, canonicalDivision, subType, detail.getData());
            } else if (MOTOR_DIVISIONS.contains(canonicalDivision)) {
                collectRemovedMotorUnitFileIds(
                        removed, persistedForm, canonicalDivision, subType, detail.getData());
            }
        }
        return new ArrayList<>(removed);
    }

    private void collectRemovedFileIds(Set<String> removed, Object previousData, Object incomingData) {
        Set<String> previousFileIds = extractFileIdsFromData(previousData);
        Set<String> incomingFileIds = extractFileIdsFromData(incomingData);
        previousFileIds.stream()
                .filter(fileId -> !incomingFileIds.contains(fileId))
                .forEach(removed::add);
    }

    private void collectRemovedPremixUnitFileIds(
            Set<String> removed,
            QcDivision persistedForm,
            String canonicalDivision,
            String subType,
            Object incomingData) {
        JsonNode incomingRoot = objectMapper.valueToTree(incomingData);
        JsonNode premixesNode = incomingRoot.get("premixes");
        if (premixesNode == null || !premixesNode.isArray()) {
            return;
        }

        Optional<DivisionDetail> persistedDetail = findDetail(persistedForm, canonicalDivision, subType);
        for (JsonNode incomingPremix : premixesNode) {
            Integer premixNo = readPremixNo(incomingPremix);
            if (premixNo == null) {
                continue;
            }
            Object previousUnit = persistedDetail
                    .flatMap(detail -> findUnit(detail, "premixes", "premixNo", premixNo, null))
                    .orElse(null);
            collectRemovedFileIds(
                    removed,
                    previousUnit,
                    objectMapper.convertValue(incomingPremix, Object.class));
        }
    }

    private void collectRemovedMotorUnitFileIds(
            Set<String> removed,
            QcDivision persistedForm,
            String canonicalDivision,
            String subType,
            Object incomingData) {
        UnitShape shape = MOTOR_UNIT_SHAPES.get(canonicalDivision);
        if (shape == null) {
            return;
        }

        JsonNode incomingRoot = objectMapper.valueToTree(incomingData);
        JsonNode unitsNode = incomingRoot.get(shape.unitsKey());
        if (unitsNode == null || !unitsNode.isArray()) {
            return;
        }

        Optional<DivisionDetail> persistedDetail = findDetail(persistedForm, canonicalDivision, subType);
        for (JsonNode incomingUnit : unitsNode) {
            String identity = readMotorIdentity(incomingUnit, shape.identityKey());
            if (identity == null || identity.isBlank()) {
                continue;
            }
            Object previousUnit = persistedDetail
                    .flatMap(detail -> findUnit(
                            detail, shape.unitsKey(), shape.identityKey(), identity, null))
                    .orElse(null);
            collectRemovedFileIds(
                    removed,
                    previousUnit,
                    objectMapper.convertValue(incomingUnit, Object.class));
        }
    }

    private Set<String> extractFileIdsFromData(Object data) {
        if (data == null) {
            return Collections.emptySet();
        }
        return FileIdExtractionUtil.extractFromJsonNode(objectMapper.valueToTree(data));
    }

    private Integer readPremixNo(JsonNode premixNode) {
        if (premixNode == null || !premixNode.has("premixNo")) {
            return null;
        }
        JsonNode value = premixNode.get("premixNo");
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.intValue() : integerValue(value.asText());
    }

    private String readMotorIdentity(JsonNode motorNode, String identityKey) {
        if (motorNode == null || identityKey == null || !motorNode.has(identityKey)) {
            return null;
        }
        JsonNode value = motorNode.get(identityKey);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText().trim();
    }

    private static final class QcFileScopeContext {
        private final Long divisionId;
        private final String division;
        private final Long sourceSubDepartmentId;
        private String motorId;
        private Integer premixNo;
        private StageType stageType;

        private QcFileScopeContext(Long divisionId, String division) {
            this.divisionId = divisionId;
            this.division = division;
            this.sourceSubDepartmentId = resolveSourceSubDepartmentId(division);
        }

        private QcFileScopeContext(
                Long divisionId,
                String division,
                Long sourceSubDepartmentId,
                String motorId,
                Integer premixNo,
                StageType stageType) {
            this.divisionId = divisionId;
            this.division = division;
            this.sourceSubDepartmentId = sourceSubDepartmentId;
            this.motorId = motorId;
            this.premixNo = premixNo;
            this.stageType = stageType;
        }

        private Long sourceSubDepartmentId() {
            return sourceSubDepartmentId;
        }

        private String motorId() {
            return motorId;
        }

        private QcFileScopeContext withNode(ObjectNode objectNode) {
            String nodeMotorId = readText(objectNode, "motorId");
            if (nodeMotorId == null) {
                nodeMotorId = readText(objectNode, "motorIdNo");
            }
            Integer nodePremixNo = readInteger(objectNode, this.premixNo);
            StageType nodeStageType = this.stageType;
            if (objectNode.hasNonNull("stageType")) {
                try {
                    nodeStageType = StageType.valueOf(objectNode.get("stageType").asText());
                } catch (IllegalArgumentException ignored) {
                    // keep inherited stageType
                }
            }
            return new QcFileScopeContext(
                    divisionId,
                    division,
                    sourceSubDepartmentId,
                    nodeMotorId != null ? nodeMotorId : motorId,
                    nodePremixNo,
                    nodeStageType);
        }

        private FileBindingScope resolveBindingScope() {
            if (motorId != null && !motorId.isBlank()) {
                return FileBindingScope.forMotor(motorId);
            }
            if (premixNo != null) {
                return FileBindingScope.forPremix(premixNo, stageType);
            }
            if (division != null || divisionId != null) {
                return FileBindingScope.builder()
                        .divisionId(divisionId)
                        .division(division)
                        .build();
            }
            return null;
        }

        private static String readText(ObjectNode objectNode, String field) {
            if (!objectNode.hasNonNull(field)) {
                return null;
            }
            String value = objectNode.get(field).asText().trim();
            return value.isEmpty() ? null : value;
        }

        private static Integer readInteger(ObjectNode objectNode, Integer fallback) {
            if (!objectNode.hasNonNull("premixNo")) {
                return fallback;
            }
            JsonNode value = objectNode.get("premixNo");
            return value.isNumber() ? value.intValue() : fallback;
        }
    }

    private static Long resolveSourceSubDepartmentId(String division) {
        if (division == null || division.isBlank()) {
            return 11L;
        }
        return QC_DIVISION_SOURCE_SUB_DEPT.getOrDefault(division.trim(), 11L);
    }

    private void markFilesReadOnlyFromNode(Object node, Long subDepartmentId) {
        if (node == null) {
            return;
        }
        Set<String> fileIds = FileIdExtractionUtil.extractFromJsonNode(objectMapper.valueToTree(node));
        fileReadOnlyService.markReadOnly(subDepartmentId, fileIds);
    }


    public QcDivisionFormResponse getFormDetails(String formId, Long subDepartmentId) {
        QcDivision form = requireForm(formId);
        if (!Objects.equals(form.getSubDepartmentId(), subDepartmentId)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "subDepartmentId does not match the QC division form");
        }
        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(form.getBatchId())
                .orElseThrow(() -> new AcemException(ErrorCode.BATCH_NOT_FOUND, "Batch not found"));
        boolean workflowUpdated = ensureWorkflow(
                form, form.getCreatedBy(), form.getCreatedAt());
        if (workflowUpdated) {
            qcDivisionRepo.save(form);
        }
        Aggregation aggregation = aggregate(form, batch);
        return buildResponse(form, batch, aggregation, null, true);
    }

    public MotorApprovalResponse changeStatusOfMotor(
            MotorApprovalRequest request,
            String approverUserId) {
        validateApprovalAction(request.getActionType(), request.getRejectionReason());
        QcDivision form = requireForm(request.getFormId());
        validateSubDepartment(form, request.getSubDepartmentId());
        String division = validateQcDivisionIdentity(
                form, request.getDivisionId(), request.getDivision(),
                request.getSubType(), true);
        if (PREMIX_DIVISIONS.contains(division) || DIVISION_SCOPED_DIVISIONS.contains(division)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "The selected QC division is not motor-scoped");
        }

        DivisionDetail detail = findDetail(form, division, normalizeNullable(request.getSubType()))
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "QC motor division data not found"));
        UnitShape shape = MOTOR_UNIT_SHAPES.get(division);
        Map<String, Object> motor = findUnit(
                detail, shape.unitsKey(), shape.identityKey(), request.getMotorId(), null)
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST, "Motor not found"));
        requireWaiting(readStatus(motor.get("motorSubmissionStatus")), "Motor");

        PersonInfo actor = personInfo(approverUserId);
        LocalDateTime now = LocalDateTime.now();
        unlockAllDivisions(form, actor, now);
        applyApproval(motor, "motorSubmissionStatus", request.getActionType(), request.getRemarks(),
                request.getRejectionReason(), actor, now);
        Batch batch = requireBatch(form.getBatchId());
        Aggregation aggregation = aggregate(form, batch);
        if (request.getActionType() == Status.APPROVED
                && divisionApproved(aggregation, division)) {
            applyDivisionApprovalAudit(detail, actor, now);
            aggregation = aggregate(form, batch);
        }
        String unlocked = request.getActionType() == Status.APPROVED
                && divisionApproved(aggregation, division)
                ? unlockSuccessor(form, division, actor, now) : null;
        if (unlocked != null) aggregation = aggregate(form, batch);
        applyAggregateStatus(form, aggregation);
        applyAutomaticCompletion(form, aggregation, actor, now);
        form.setUpdatedBy(actor);
        form.setUpdatedAt(now);
        qcDivisionRepo.save(form);
        syncApproval(form, aggregation, approverUserId, now,
                request.getActionType() == Status.APPROVED
                        && hasStfEligibleMotor(form, batch, aggregation));
        if (request.getActionType() == Status.APPROVED) {
            markFilesReadOnlyFromNode(motor, form.getSubDepartmentId());
        }
        if (form.getStatus() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }

        QcMotorStatusDto unitStatus = aggregation.motorStatuses().stream()
                .filter(status -> Objects.equals(status.getDivision(), division)
                        && Objects.equals(status.getSubType(), normalizeNullable(request.getSubType()))
                        && Objects.equals(status.getMotorId(), request.getMotorId()))
                .findFirst().orElse(null);
        DivisionStatusDto divisionStatus = aggregation.divisionStatuses().stream()
                .filter(status -> Objects.equals(status.getDivision(), division))
                .findFirst().orElse(null);

        return MotorApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .division(division)
                .subType(normalizeNullable(request.getSubType()))
                .motorId(request.getMotorId())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .motorSubmissionStatus(unitStatus != null ? unitStatus.getMotorSubmissionStatus() : request.getActionType())
                .batchStatus(form.getStatus())
                .divisionStatus(divisionStatus != null ? divisionStatus.getStatus() : null)
                .allDivisionsApproved(aggregation.allDivisionsApproved())
                .unlockedDivision(unlocked)
                .allMotorsApproved(aggregation.allMotorsApproved())
                .pendingMotorCount(aggregation.pendingMotorCount())
                .approvedMotorCount(aggregation.approvedMotorCount())
                .rejectedMotorCount(aggregation.rejectedMotorCount())
                .totalMotorCount(aggregation.totalMotorCount())
                .remarks(request.getRemarks())
                .rejectionReason(request.getRejectionReason())
                .actionBy(actor.getFullName())
                .actionAt(now)
                .build();
    }

    public PremixApprovalResponse changeStatusOfPremix(
            PremixApprovalRequest request,
            String approverUserId) {
        validateApprovalAction(request.getActionType(), request.getRejectionReason());
        QcDivision form = requireForm(request.getFormId());
        validateSubDepartment(form, request.getSubDepartmentId());
        String division = validateQcDivisionIdentity(
                form, request.getDivisionId(), request.getDivision(),
                request.getSubType(), true);
        if (!PREMIX_DIVISIONS.contains(division)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "The selected QC division is not premix-scoped");
        }
        String subType = normalizeNullable(request.getSubType());
        String stageType = request.getStageType() != null ? request.getStageType().name() : subType;
        if ("MIXING".equals(division)
                && !"PREMIX".equals(stageType) && !"FINAL_MIX".equals(stageType)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "stageType is required for MIXING approval");
        }
        DivisionDetail detail;
        Map<String, Object> premix;
        if ("MIXING".equals(division)) {
            MixingApprovalTarget target = findMixingApprovalTarget(
                    form, subType, stageType, request.getPremixNo());
            detail = target.detail();
            premix = target.stageDetails();
        } else {
            detail = findDetail(form, division, subType)
                    .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                            "QC premix division data not found"));
            premix = findUnit(detail, "premixes", "premixNo", request.getPremixNo(), null)
                    .orElseThrow(() -> new AcemException(
                            ErrorCode.INVALID_REQUEST, "Premix not found"));
        }
        requireWaiting(readStatus(premix.get("premixSubmissionStatus")), "Premix");

        PersonInfo actor = personInfo(approverUserId);
        LocalDateTime now = LocalDateTime.now();
        unlockAllDivisions(form, actor, now);
        applyApproval(premix, "premixSubmissionStatus", request.getActionType(), request.getRemarks(),
                request.getRejectionReason(), actor, now);
        Batch batch = requireBatch(form.getBatchId());
        Aggregation aggregation = aggregate(form, batch);
        if (request.getActionType() == Status.APPROVED
                && divisionApproved(aggregation, division)) {
            applyDivisionApprovalAudit(detail, actor, now);
            aggregation = aggregate(form, batch);
        }
        String unlocked = request.getActionType() == Status.APPROVED
                && divisionApproved(aggregation, division)
                ? unlockSuccessor(form, division, actor, now) : null;
        if (unlocked != null) aggregation = aggregate(form, batch);
        applyAggregateStatus(form, aggregation);
        applyAutomaticCompletion(form, aggregation, actor, now);
        form.setUpdatedBy(actor);
        form.setUpdatedAt(now);
        qcDivisionRepo.save(form);
        syncApproval(form, aggregation, approverUserId, now,
                request.getActionType() == Status.APPROVED
                        && hasStfEligibleMotor(form, batch, aggregation));
        if (request.getActionType() == Status.APPROVED) {
            markFilesReadOnlyFromNode(premix, form.getSubDepartmentId());
        }
        if (form.getStatus() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }
        DivisionStatusDto divisionStatus = aggregation.divisionStatuses().stream()
                .filter(status -> Objects.equals(status.getDivision(), division))
                .findFirst().orElse(null);

        return PremixApprovalResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .division(division)
                .subType(subType)
                .premixNo(request.getPremixNo())
                .stageType(request.getStageType())
                .actionType(request.getActionType())
                .status(request.getActionType())
                .premixSubmissionStatus(request.getActionType())
                .batchStatus(form.getStatus())
                .divisionStatus(divisionStatus != null ? divisionStatus.getStatus() : null)
                .allDivisionsApproved(aggregation.allDivisionsApproved())
                .unlockedDivision(unlocked)
                .allPremixesApproved(aggregation.allPremixesApproved())
                .pendingPremixCount(aggregation.pendingPremixCount())
                .approvedPremixCount(aggregation.approvedPremixCount())
                .rejectedPremixCount(aggregation.rejectedPremixCount())
                .totalPremixCount(aggregation.totalPremixCount())
                .remarks(request.getRemarks())
                .rejectionReason(request.getRejectionReason())
                .actionBy(actor.getFullName())
                .actionAt(now)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public QcDivisionFormResponse changeStatusOfDivision(
            QcDivisionApprovalRequest request,
            String approverUserId) {
        validateApprovalAction(request.getActionType(), request.getRejectionReason());
        QcDivision form = requireForm(request.getFormId());
        validateSubDepartment(form, request.getSubDepartmentId());
        String division = validateQcDivisionIdentity(
                form, request.getDivisionId(), request.getDivision(),
                request.getSubType(), true);

        Object targetIdentifier = null;
        Object blockchainPayload = null;
        LocalDateTime now = LocalDateTime.now(); // Capture timestamp for audit consistency
        if (PREMIX_DIVISIONS.contains(division)) {
            if (request.getPremixNo() == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "premixNo is required for QC division " + division);
            }
            if (request.getMotorId() != null && !request.getMotorId().isBlank()) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorId is not allowed for premix-scoped QC division " + division);
            }
            if ("MIXING".equals(division) && request.getStageType() == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "stageType is required for MIXING approval");
            }

            if ("MIXING".equals(division)) {
                String stageTypeStr = String.valueOf(request.getStageType());
                targetIdentifier = stageTypeStr + request.getPremixNo();
            } else {
                targetIdentifier = "PREMIX "+ request.getPremixNo();
            }


            if (request.getActionType() == Status.APPROVED) {
                try {
                    blockchainPayload = buildDivisionBlockchainPayload(
                            request.getFormId(), division, request.getSubType(), targetIdentifier, approverUserId, now);

                    approvalBlockchainAssetService.createAssetOnBlockchain(
                            form.getSubDepartmentId(),
                            form.getBatchId(),
                            form.getFormId(),
                            request.getDivisionId(), // Pass division context for 4-part asset ID format
                            blockchainPayload,
                            targetIdentifier,
                            approverUserId);
                } catch (Exception e) {
                    markRollbackOnlyQuietly();
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
                }
            }

            PremixApprovalResponse approval = changeStatusOfPremix(
                    PremixApprovalRequest.builder()
                            .formId(request.getFormId())
                            .divisionId(request.getDivisionId())
                            .division(division)
                            .subType(request.getSubType())
                            .premixNo(request.getPremixNo())
                            .stageType(request.getStageType())
                            .subDepartmentId(request.getSubDepartmentId())
                            .actionType(request.getActionType())
                            .remarks(request.getRemarks())
                            .rejectionReason(request.getRejectionReason())
                            .build(),
                    approverUserId);

            return unifiedPremixApprovalResponse(request, division, approval);
        }

        if (MOTOR_DIVISIONS.contains(division)) {
            if (request.getMotorId() == null || request.getMotorId().isBlank()) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "motorId is required for QC division " + division);
            }
            if (request.getPremixNo() != null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "premixNo is not allowed for motor-scoped QC division " + division);
            }

            targetIdentifier = request.getMotorId();
            if (request.getActionType() == Status.APPROVED) {
                try {
                    blockchainPayload = buildDivisionBlockchainPayload(
                            request.getFormId(), division, request.getSubType(), targetIdentifier , approverUserId, now);
                    approvalBlockchainAssetService.createAssetOnBlockchain(
                            form.getSubDepartmentId(),
                            form.getBatchId(),
                            form.getFormId(),
                            request.getDivisionId(), // Pass division context for 4-part asset ID format
                            blockchainPayload,
                            targetIdentifier,
                            approverUserId);
                } catch (Exception e) {
                    markRollbackOnlyQuietly();
                    throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
                }
            }
            MotorApprovalResponse approval = changeStatusOfMotor(
                    MotorApprovalRequest.builder()
                            .formId(request.getFormId())
                            .divisionId(request.getDivisionId())
                            .division(division)
                            .subType(request.getSubType())
                            .motorId(request.getMotorId())
                            .subDepartmentId(request.getSubDepartmentId())
                            .actionType(request.getActionType())
                            .remarks(request.getRemarks())
                            .rejectionReason(request.getRejectionReason())
                            .build(),
                    approverUserId);
            return unifiedMotorApprovalResponse(request, division, approval);
        }

        if (request.getPremixNo() != null
                || (request.getMotorId() != null && !request.getMotorId().isBlank())) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "premixNo and motorId are not allowed for division-scoped QC approval");
        }

        if (request.getActionType() == Status.APPROVED) {
            try {
                blockchainPayload = buildDivisionBlockchainPayload(
                        request.getFormId(), division, request.getSubType(), null , approverUserId, now);
                approvalBlockchainAssetService.createAssetOnBlockchain(
                        form.getSubDepartmentId(),
                        form.getBatchId(),
                        form.getFormId(),
                        request.getDivisionId(), // Pass division context for 3-part asset ID format
                        blockchainPayload,
                        null, // No sub-identifier
                        approverUserId);

            } catch (Exception e) {
                markRollbackOnlyQuietly();
                throw new AcemException(ErrorCode.INVALID_REQUEST, "Blockchain sync failed: " + e.getMessage());
            }
        }

        return changeStatusOfDivisionScoped(request, form, division, approverUserId);
    }

    /** Mark current txn rollback-only when present; never mask the real failure with NoTransactionException. */
    private void markRollbackOnlyQuietly() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // No Spring-managed transaction in scope (e.g. caller without @Transactional).
        }
    }

    private QcDivisionFormResponse changeStatusOfDivisionScoped(
            QcDivisionApprovalRequest request,
            QcDivision form,
            String division,
            String approverUserId) {
        DivisionDetail detail = findDetail(form, division, null)
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "QC division data not found"));
        requireWaiting(detail.getDivisionSubmissionStatus(), "Division");

        PersonInfo actor = personInfo(approverUserId);
        LocalDateTime now = LocalDateTime.now();
        unlockAllDivisions(form, actor, now);
        detail.setDivisionSubmissionStatus(request.getActionType());
        detail.setRemarks(request.getActionType() == Status.APPROVED ? request.getRemarks() : null);
        detail.setRejectionReason(request.getActionType() == Status.REJECTED
                ? request.getRejectionReason() : null);
        if (request.getActionType() == Status.APPROVED) {
            applyDivisionApprovalAudit(detail, actor, now);
        } else {
            clearDivisionApprovedAudit(detail);
        }
        Batch batch = requireBatch(form.getBatchId());
        Aggregation aggregation = aggregate(form, batch);
        String unlocked = request.getActionType() == Status.APPROVED
                && divisionApproved(aggregation, division)
                ? unlockSuccessor(form, division, actor, now) : null;
        if (unlocked != null) aggregation = aggregate(form, batch);
        applyAggregateStatus(form, aggregation);
        applyAutomaticCompletion(form, aggregation, actor, now);
        form.setUpdatedBy(actor);
        form.setUpdatedAt(now);
        qcDivisionRepo.save(form);
        syncApproval(form, aggregation, approverUserId, now,
                request.getActionType() == Status.APPROVED
                        && hasStfEligibleMotor(form, batch, aggregation));
        if (request.getActionType() == Status.APPROVED) {
            markFilesReadOnlyFromNode(detail.getData(), form.getSubDepartmentId());
        }
        if (form.getStatus() == Status.COMPLETELY_APPROVED) {
            fileReadOnlyService.markReadOnlyForForm(form.getFormId(), form.getSubDepartmentId());
        }
        ActionContext action = new ActionContext(request.getDivisionId(), division, null, null, null, null,
                null, null, null, null, detail.getDivisionSubmissionType(),
                detail.getDivisionSubmissionStatus(), unlocked);
        return buildResponse(form, batch, aggregation, action, false);
    }

    private QcDivisionFormResponse unifiedPremixApprovalResponse(
            QcDivisionApprovalRequest request,
            String division,
            PremixApprovalResponse approval) {
        QcDivisionFormResponse response = getFormDetails(
                request.getFormId(), request.getSubDepartmentId());
        return response;
    }

    private QcDivisionFormResponse unifiedMotorApprovalResponse(
            QcDivisionApprovalRequest request,
            String division,
            MotorApprovalResponse approval) {
        return getFormDetails(
                request.getFormId(), request.getSubDepartmentId());
    }

    private Batch requireActiveBatch(String batchId, Long subDepartmentId) {
        Batch batch = requireBatch(batchId);
        batchService.assertStageUnlockedAndActive(batch, subDepartmentId, "QC Division");
        return batch;
    }

    private Batch requireBatch(String batchId) {
        return batchRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.BATCH_NOT_FOUND, "No batch exists for batchId: " + batchId));
    }

    private QcDivision requireForm(String formId) {
        return qcDivisionRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(
                        ErrorCode.QC_FORM_NOT_FOUND,
                        "QC division form not found",
                        "No QC form exists for formId " + formId));
    }

    private void validateFormIdentity(QcDivision form, String batchId, Long subDepartmentId) {
        if (!Objects.equals(form.getBatchId(), batchId)
                || !Objects.equals(form.getSubDepartmentId(), subDepartmentId)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "formId, batchId, and subDepartmentId do not identify the same QC form");
        }
    }

    private void validateSubDepartment(QcDivision form, Long subDepartmentId) {
        if (!Objects.equals(form.getSubDepartmentId(), subDepartmentId)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "subDepartmentId does not match the QC division form");
        }
    }

    private PersonInfo personInfo(String userId) {
        User user = userRepo.findByUserId(userId)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return PersonInfo.builder().id(userId).fullName(user.getUsername()).build();
    }

    private List<QcDivisionWorkflow> buildWorkflow(Batch batch, PersonInfo actor, LocalDateTime now) {
        List<QcDivisionWorkflow> workflow = new ArrayList<>();
        List<QcDivisionMaster> masters = qcDivisionMasterRepo.findAllByOrderByDisplayOrderAsc();
        int sequence = 1;
        for (QcDivisionMaster master : masters) {
            if (Boolean.TRUE.equals(master.getDisabled())) continue;
            List<QcDivisionMaster.DivisionType> types = Optional.ofNullable(master.getTypes())
                    .orElseGet(Collections::emptyList).stream()
                    .filter(type -> !Boolean.TRUE.equals(type.getDisabled()))
                    .sorted(Comparator.comparing(QcDivisionMaster.DivisionType::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (!types.isEmpty()) {
                for (QcDivisionMaster.DivisionType type : types) {
                    String division = QcDivisionCatalog.canonicalName(type.getDivisionId());
                    if (division != null) {
                        workflow.add(newWorkflow(
                                batch, type.getDivisionId(), division, sequence++, actor, now));
                    }
                }
            } else {
                String division = QcDivisionCatalog.canonicalName(master.getDivisionId());
                if (division != null) {
                    workflow.add(newWorkflow(
                            batch, master.getDivisionId(), division, sequence++, actor, now));
                }
            }
        }
        return workflow;
    }

    private QcDivisionWorkflow newWorkflow(
            Batch batch,
            Long divisionId,
            String division,
            int sequence,
            PersonInfo actor,
            LocalDateTime now) {
        boolean unlocked = isDivisionInitiallyUnlocked(batch, division);
        return QcDivisionWorkflow.builder()
                .divisionId(divisionId)
                .division(division)
                .sequence(sequence)
                .locked(!unlocked)
                .status(unlocked ? Status.TO_BE_INITIATED : Status.YET_TO_START)
                .unlockedBy(unlocked ? actor : null)
                .unlockedAt(unlocked ? now : null)
                .build();
    }

    private boolean isDivisionInitiallyUnlocked(Batch batch, String division) {
        if (QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            return QcDivisionCatalog.isExperimentalUnlocked(division);
        }
        if (QcDivisionProgressHelper.isQualificationSubscale(batch)) {
            return true;
        }
        // MAIN parallel: only revalidation at create.
        if (MainBatchStageProgressHelper.isParallelMainFlow(batch)) {
            return QcDivisionCatalog.RAW_MATERIAL_REVALIDATION.equals(division);
        }
        return true;
    }

    private boolean ensureWorkflow(QcDivision form, PersonInfo actor, LocalDateTime now) {
        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(form.getBatchId()).orElse(null);
        boolean updated = false;
        if (form.getDivisionWorkflow() == null || form.getDivisionWorkflow().isEmpty()) {
            form.setDivisionWorkflow(buildWorkflow(batch, actor, now));
            updated = true;
        } else {
            updated = appendMissingWorkflowDivisions(form, batch, actor, now)
                    | syncWorkflowLocksFromStageProgress(form, batch, actor, now);
        }
        if (form.getDivisionDetails() == null) {
            form.setDivisionDetails(new ArrayList<>());
            updated = true;
        }
        return updated;
    }

    /**
     * Append master divisions missing from an older form workflow (e.g. STATIC_TEST_FACILITY).
     */
    private boolean appendMissingWorkflowDivisions(
            QcDivision form,
            Batch batch,
            PersonInfo actor,
            LocalDateTime now) {
        List<QcDivisionWorkflow> workflow = form.getDivisionWorkflow();
        if (workflow == null) {
            return false;
        }
        Set<Long> existingIds = workflow.stream()
                .map(QcDivisionWorkflow::getDivisionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> existingNames = workflow.stream()
                .map(row -> QcDivisionCatalog.normalizeDivisionName(row.getDivision()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        boolean updated = false;
        int sequence = workflow.stream()
                .map(QcDivisionWorkflow::getSequence)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(workflow.size()) + 1;
        List<QcDivisionWorkflow> fresh = buildWorkflow(batch, actor, now);
        for (QcDivisionWorkflow candidate : fresh) {
            Long id = candidate.getDivisionId();
            String name = QcDivisionCatalog.normalizeDivisionName(candidate.getDivision());
            if ((id != null && existingIds.contains(id))
                    || (name != null && existingNames.contains(name))) {
                continue;
            }
            candidate.setSequence(sequence++);
            // Align lock with current batch unlock state for STF family / NDT.
            boolean unlocked = QcDivisionProgressHelper.isDivisionUnlocked(batch, name);
            if (QcDivisionCatalog.NDT.equals(name) && !hasManufacturingNdtApprovalUnlock(batch)) {
                unlocked = false;
            }
            // Match syncWorkflowLocksFromStageProgress: Qual/Experimental skip BEM force-lock.
            if (isStfFamilyDivision(name)
                    && !QcDivisionProgressHelper.isQualificationSubscale(batch)
                    && !QcDivisionProgressHelper.isExperimentalSubscale(batch)
                    && !MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
                unlocked = false;
            }
            candidate.setLocked(!unlocked);
            candidate.setStatus(unlocked ? Status.TO_BE_INITIATED : Status.YET_TO_START);
            if (unlocked) {
                candidate.setUnlockedBy(actor);
                candidate.setUnlockedAt(now);
            }
            workflow.add(candidate);
            if (id != null) {
                existingIds.add(id);
            }
            if (name != null) {
                existingNames.add(name);
            }
            updated = true;
        }
        return updated;
    }

    private boolean syncWorkflowLocksFromStageProgress(
            QcDivision form,
            Batch batch,
            PersonInfo actor,
            LocalDateTime now) {
        if (batch == null || batch.getStageProgress() == null) {
            return false;
        }
        boolean updated = false;
        for (QcDivisionWorkflow workflow : Optional.ofNullable(form.getDivisionWorkflow())
                .orElseGet(Collections::emptyList)) {
            String division = QcDivisionCatalog.normalizeDivisionName(workflow.getDivision());
            if (division != null && !Objects.equals(workflow.getDivision(), division)) {
                workflow.setDivision(division);
                updated = true;
            }
            boolean shouldUnlock = QcDivisionProgressHelper.isDivisionUnlocked(batch, division);
            // NDT: only unlock after manufacturing NDT APPROVED.
            if (QcDivisionCatalog.NDT.equals(division)
                    && !QcDivisionProgressHelper.isQualificationSubscale(batch)
                    && !QcDivisionProgressHelper.isExperimentalSubscale(batch)
                    && !hasManufacturingNdtApprovalUnlock(batch)) {
                shouldUnlock = false;
                if (!workflow.isLocked() || workflow.getStatus() != Status.YET_TO_START) {
                    workflow.setLocked(true);
                    workflow.setStatus(Status.YET_TO_START);
                    workflow.setUnlockedBy(null);
                    workflow.setUnlockedAt(null);
                    updated = true;
                }
                continue;
            }
            // STF / QC / WEIGHTMENT: only unlock after any STF BEM motor is APPROVED.
            if (isStfFamilyDivision(division)
                    && !QcDivisionProgressHelper.isQualificationSubscale(batch)
                    && !QcDivisionProgressHelper.isExperimentalSubscale(batch)
                    && !MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
                shouldUnlock = false;
                if (!workflow.isLocked() || workflow.getStatus() != Status.YET_TO_START) {
                    workflow.setLocked(true);
                    workflow.setStatus(Status.YET_TO_START);
                    workflow.setUnlockedBy(null);
                    workflow.setUnlockedAt(null);
                    updated = true;
                }
                continue;
            }
            if (shouldUnlock && workflow.isLocked()) {
                workflow.setLocked(false);
                if (workflow.getStatus() == Status.YET_TO_START) {
                    workflow.setStatus(Status.TO_BE_INITIATED);
                }
                workflow.setUnlockedBy(actor);
                workflow.setUnlockedAt(now);
                updated = true;
            }
        }
        return updated;
    }

    private void unlockAllDivisions(
            QcDivision form,
            PersonInfo actor,
            LocalDateTime now) {
        // Prefer stageProgress-driven unlocks; keep legacy helper for approval paths that still call it.
        Batch batch = batchRepo.findByBatchIdAndIsDeletedFalse(form.getBatchId()).orElse(null);
        syncWorkflowLocksFromStageProgress(form, batch, actor, now);
    }

    private ActionContext mergeDivisionPayloads(
            QcDivision form,
            Batch batch,
            List<Map<String, Object>> requestedDetails,
            PersonInfo actor,
            LocalDateTime now) {
        if (requestedDetails == null || requestedDetails.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "divisionDetails must contain at least one division");
        }
        List<ActionContext> savedActions = new ArrayList<>();
        Set<String> received = new HashSet<>();
        for (Map<String, Object> request : requestedDetails) {
            String requestedDivision = normalizeDivision(stringValue(request.get("division")));
            String subType = normalizeNullable(stringValue(request.get("subType")));
            String division = resolvePayloadDivision(requestedDivision, subType);
            String storedDivision = "RAW_MATERIAL".equals(requestedDivision)
                    ? requestedDivision : division;
            if (!received.add(division + "|" + subType)) {
                throw new AcemException(ErrorCode.INVALID_REQUEST,
                        "Duplicate QC division payload: " + division);
            }
            QcDivisionWorkflow workflow = workflow(form, division);
            Map<String, Object> data = new LinkedHashMap<>(
                    mapValue(request.get("data"), "data is required for " + division));
            List<ActionContext> actions;
            if (DIVISION_SCOPED_DIVISIONS.contains(division)) {
                actions = mergeDivisionScopedPayload(
                        form, workflow, request, storedDivision, subType, data, actor, now);
            } else if ("MIXING".equals(division)) {
                actions = mergeMixingPayload(
                        form, batch, workflow, storedDivision, subType, data, actor, now);
            } else if (PREMIX_DIVISIONS.contains(division)) {
                actions = mergePremixPayload(
                        form, batch, workflow, storedDivision, subType, data, actor, now);
            } else if (MOTOR_DIVISIONS.contains(division)) {
                actions = mergeMotorPayload(
                        form, batch, workflow, storedDivision, subType, data, actor, now);
            } else {
                throw new AcemException(ErrorCode.INVALID_REQUEST,
                        "Unsupported QC division: " + division);
            }
            savedActions.addAll(actions);
        }
        return savedActions.size() == 1 ? savedActions.getFirst() : null;
    }

    private List<ActionContext> mergeDivisionScopedPayload(
            QcDivision form,
            QcDivisionWorkflow workflow,
            Map<String, Object> request,
            String storedDivision,
            String subType,
            Map<String, Object> data,
            PersonInfo actor,
            LocalDateTime now) {
        DivisionDetail detail = getOrCreateDetail(
                form, workflow, storedDivision, subType, null);
        Status submissionType = optionalSubmissionStatus(
                request.get("divisionSubmissionType"), "divisionSubmissionType");
        boolean roundTrip = sameWorkflowState(request,
                "divisionSubmissionType", "divisionSubmissionStatus",
                detail.getDivisionSubmissionType(), detail.getDivisionSubmissionStatus());
        if (roundTrip && isApprovalProtected(detail.getDivisionSubmissionStatus())
                && !Objects.equals(detail.getData(), data)) {
            requireEditable(detail.getDivisionSubmissionStatus(),
                    "Division " + workflow.getDivision());
        }
        if (submissionType != null && !roundTrip) {
            requireUnlocked(form, workflow);
            requireEditable(detail.getDivisionSubmissionStatus(),
                    "Division " + workflow.getDivision());
            detail.setDivisionSubmissionType(submissionType);
            detail.setDivisionSubmissionStatus(statusFor(submissionType));
            clearDivisionApproval(detail);
            applySubmissionAudit(detail, submissionType, actor, now);
        }
        detail.setData(mergeData(detail, data));
        if (submissionType == null || roundTrip) return Collections.emptyList();
        return List.of(new ActionContext(workflow.getDivisionId(), workflow.getDivision(), subType,
                null, null, null, null, null, null, null, submissionType,
                detail.getDivisionSubmissionStatus(), null));
    }

    private List<ActionContext> mergePremixPayload(
            QcDivision form,
            Batch batch,
            QcDivisionWorkflow workflow,
            String storedDivision,
            String subType,
            Map<String, Object> data,
            PersonInfo actor,
            LocalDateTime now) {
        List<Map<String, Object>> incomingUnits = copiedMapList(
                data.get("premixes"), "data.premixes is required for " + workflow.getDivision());
        // Capture before merge — existing DB rows may already have weightmentSheet: null.
        Object incomingWeightmentSheet = data.get("weightmentSheet");
        DivisionDetail detail = getOrCreateDetail(
                form, workflow, storedDivision, subType, "premixes");
        data = mergeData(detail, data);
        List<Map<String, Object>> merged = mergeUntouchedData(detail, "premixes");
        List<ActionContext> actions = new ArrayList<>();
        String premixDivision = QcDivisionCatalog.normalizeDivisionName(workflow.getDivision());
        for (Map<String, Object> incoming : incomingUnits) {
            Integer premixNo = validatePremixNo(batch, incoming.get("premixNo"));
            // Per-premix unlock (manufacturing RMP APPROVED), not whole-division unlock.
            mainBatchGateValidator.assertQcPremixUnitUnlocked(
                    batch, premixDivision, premixNo, StageType.PREMIX);
            Status submissionType = optionalSubmissionStatus(
                    incoming.get("premixSubmissionType"), "premixSubmissionType");
            Optional<Map<String, Object>> existing = findUnit(
                    detail, "premixes", "premixNo", premixNo, null);
            boolean roundTrip = sameWorkflowState(incoming,
                    "premixSubmissionType", "premixSubmissionStatus",
                    existing.orElse(null));
            ensureProtectedRoundTripUnchanged(roundTrip, incoming, existing.orElse(null),
                    "Premix " + premixNo + " in " + workflow.getDivision(),
                    "premixSubmissionStatus");
            if (submissionType != null && !roundTrip) {
                requireUnlocked(form, workflow);
                existing.ifPresent(unit -> requireEditable(
                        readStatus(unit.get("premixSubmissionStatus")),
                        "Premix " + premixNo + " in " + workflow.getDivision()));
                incoming.put("premixSubmissionType", submissionType);
                incoming.put("premixSubmissionStatus", statusFor(submissionType));
                clearUnitApproval(incoming);
                applySubmissionAudit(incoming, submissionType, actor, now);
                applySubmissionAudit(detail, submissionType, actor, now);
                actions.add(new ActionContext(workflow.getDivisionId(), workflow.getDivision(), subType,
                        null, premixNo, null, null, null, submissionType,
                        statusFor(submissionType), null, null, null));
            } else {
                retainUnitWorkflowFields(incoming, existing.orElse(null));
            }
            replaceUnit(merged, incoming,
                    unit -> Objects.equals(integerValue(unit.get("premixNo")), premixNo));
        }
        data.put("premixes", merged);
        // Always persist non-null weighment from this request (do not keep stale null).
        if (incomingWeightmentSheet != null) {
            data.put("weightmentSheet", incomingWeightmentSheet);
        }
        detail.setData(data);
        return actions;
    }

    private List<ActionContext> mergeMixingPayload(
            QcDivision form,
            Batch batch,
            QcDivisionWorkflow workflow,
            String storedDivision,
            String subType,
            Map<String, Object> data,
            PersonInfo actor,
            LocalDateTime now) {
        List<Map<String, Object>> incomingUnits = copiedMapList(
                data.get("premixes"), "data.premixes is required for MIXING");
        DivisionDetail detail = getOrCreateDetail(
                form, workflow, storedDivision, subType, "premixes");
        data = mergeData(detail, data);
        List<Map<String, Object>> merged = mergeUntouchedData(detail, "premixes");
        List<ActionContext> actions = new ArrayList<>();
        for (Map<String, Object> incoming : incomingUnits) {
            Integer premixNo = validatePremixNo(batch, incoming.get("premixNo"));
            Map<String, Object> existing = findUnit(
                    detail, "premixes", "premixNo", premixNo, null).orElse(null);

            /*
             * Support the earlier Mixing payload shape where the submission
             * type was placed on the premix container. It is safe to infer the
             * target only when stageType is supplied or exactly one stage
             * details object is present.
             */
            Status containerSubmissionType = optionalSubmissionStatus(
                    incoming.get("premixSubmissionType"), "premixSubmissionType");
            String containerStageType = normalizeNullable(stringValue(incoming.get("stageType")));
            if (containerSubmissionType != null && containerStageType == null) {
                List<String> suppliedStages = Stream.of("PREMIX", "FINAL_MIX")
                        .filter(type -> incoming.get(mixingDetailsKey(type)) instanceof Map<?, ?>)
                        .toList();
                if (suppliedStages.size() != 1) {
                    throw new AcemException(
                            ErrorCode.INVALID_REQUEST,
                            "stageType is required when a Mixing premixSubmissionType applies "
                                    + "to more than one stage");
                }
                containerStageType = suppliedStages.getFirst();
            }
            if (containerStageType != null
                    && !"PREMIX".equals(containerStageType)
                    && !"FINAL_MIX".equals(containerStageType)) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "stageType must be PREMIX or FINAL_MIX");
            }

            Map<String, Object> mergedIncoming = new LinkedHashMap<>();
            if (existing != null) {
                mergedIncoming.putAll(existing);
            }
            mergedIncoming.putAll(incoming);
            incoming.clear();
            incoming.putAll(mergedIncoming);
            incoming.remove("premixSubmissionType");
            incoming.remove("premixSubmissionStatus");

            for (String stageType : List.of("PREMIX", "FINAL_MIX")) {
                String detailsKey = mixingDetailsKey(stageType);
                Object stageValue = incoming.get(detailsKey);
                if (!(stageValue instanceof Map<?, ?>)) continue;
                StageType stageEnum = "FINAL_MIX".equals(stageType)
                        ? StageType.FINAL_MIX : StageType.PREMIX;
                // Per Premix / Final Mix unlock after manufacturing Mixing APPROVED.
                mainBatchGateValidator.assertQcPremixUnitUnlocked(
                        batch, "MIXING", premixNo, stageEnum);
                Map<String, Object> stage = new LinkedHashMap<>(mapValue(stageValue,
                        detailsKey + " must be an object"));
                Map<String, Object> existingStage = nestedMap(existing, detailsKey);
                Status submissionType = optionalSubmissionStatus(
                        stage.get("premixSubmissionType"), detailsKey + ".premixSubmissionType");
                if (containerSubmissionType != null
                        && stageType.equals(containerStageType)) {
                    if (submissionType != null && submissionType != containerSubmissionType) {
                        throw new AcemException(
                                ErrorCode.INVALID_REQUEST,
                                "Conflicting premixSubmissionType values for " + detailsKey);
                    }
                    submissionType = containerSubmissionType;
                    stage.put("premixSubmissionType", submissionType);
                }
                boolean roundTrip = sameWorkflowState(stage,
                        "premixSubmissionType", "premixSubmissionStatus", existingStage);
                ensureProtectedRoundTripUnchanged(roundTrip, stage, existingStage,
                        stageType + " " + premixNo + " in MIXING",
                        "premixSubmissionStatus");
                if (submissionType != null && !roundTrip) {
                    requireUnlocked(form, workflow);
                    requireEditable(existingStage == null ? null
                                    : readStatus(existingStage.get("premixSubmissionStatus")),
                            stageType + " " + premixNo + " in MIXING");
                    stage.put("premixSubmissionType", submissionType);
                    stage.put("premixSubmissionStatus", statusFor(submissionType));
                    clearUnitApproval(stage);
                    applySubmissionAudit(stage, submissionType, actor, now);
                    applySubmissionAudit(detail, submissionType, actor, now);
                    actions.add(new ActionContext(workflow.getDivisionId(), workflow.getDivision(), subType,
                            null, premixNo, stageType, null, null, submissionType,
                            statusFor(submissionType), null, null, null));
                } else {
                    retainUnitWorkflowFields(stage, existingStage);
                }
                incoming.put(detailsKey, stage);
            }
            replaceUnit(merged, incoming,
                    unit -> Objects.equals(integerValue(unit.get("premixNo")), premixNo));
        }
        data.put("premixes", merged);
        detail.setData(data);
        return actions;
    }

    private List<ActionContext> mergeMotorPayload(
            QcDivision form,
            Batch batch,
            QcDivisionWorkflow workflow,
            String storedDivision,
            String subType,
            Map<String, Object> data,
            PersonInfo actor,
            LocalDateTime now) {
        String motorDivision = QcDivisionCatalog.normalizeDivisionName(workflow.getDivision());
        UnitShape shape = MOTOR_UNIT_SHAPES.get(motorDivision);
        if (shape == null) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Unsupported QC division: " + workflow.getDivision());
        }
        List<Map<String, Object>> incomingUnits = copiedMapList(data.get(shape.unitsKey()),
                "data." + shape.unitsKey() + " is required for " + motorDivision);
        DivisionDetail detail = getOrCreateDetail(
                form, workflow, storedDivision, subType, shape.unitsKey());
        data = mergeData(detail, data);
        List<Map<String, Object>> merged = mergeUntouchedData(detail, shape.unitsKey());
        List<ActionContext> actions = new ArrayList<>();
        for (Map<String, Object> incoming : incomingUnits) {
            if ("TRIMMING".equals(motorDivision)) {
                normalizeTrimmingMotorPayload(incoming);
            }
            String motorId = stringValue(incoming.get(shape.identityKey()));
            validateMotorId(batch, motorId, shape.identityKey(), motorDivision);
            // QC unlock is per division unit (divisionStatuses), not QC stage motorStatuses.
            // Stage-level assertMotorUnlocked(QC) stays YET_TO_START even after Case Prep
            // unlocks HARDWARE for this motor.
            mainBatchGateValidator.assertQcMotorUnitUnlocked(batch, motorDivision, motorId);
            Status submissionType = optionalSubmissionStatus(
                    incoming.get("motorSubmissionType"), "motorSubmissionType");
            Optional<Map<String, Object>> existing = findUnit(
                    detail, shape.unitsKey(), shape.identityKey(), motorId, null);
            if ("POST_CURE".equals(motorDivision)) {
                mergePostCureMotorSections(incoming, existing.orElse(null));
            }
            boolean roundTrip = sameWorkflowState(incoming,
                    "motorSubmissionType", "motorSubmissionStatus", existing.orElse(null));
            ensureProtectedRoundTripUnchanged(roundTrip, incoming, existing.orElse(null),
                    "Motor " + motorId + " in " + motorDivision,
                    "motorSubmissionStatus");
            if (submissionType != null && !roundTrip) {
                requireUnlocked(form, workflow);
                existing.ifPresent(unit -> requireEditable(
                        readStatus(unit.get("motorSubmissionStatus")),
                        "Motor " + motorId + " in " + motorDivision));
                incoming.put("motorSubmissionType", submissionType);
                incoming.put("motorSubmissionStatus", statusFor(submissionType));
                clearUnitApproval(incoming);
                applySubmissionAudit(incoming, submissionType, actor, now);
                applySubmissionAudit(detail, submissionType, actor, now);
                actions.add(new ActionContext(workflow.getDivisionId(), motorDivision, subType,
                        motorId, null, null, submissionType, statusFor(submissionType),
                        null, null, null, null, null));
            } else {
                retainUnitWorkflowFields(incoming, existing.orElse(null));
            }
            replaceUnit(merged, incoming,
                    unit -> Objects.equals(stringValue(unit.get(shape.identityKey())), motorId));
        }
        if ("POST_CURE".equals(motorDivision)) {
            validateQcPostCureMotor.validateMotors(merged);
        }
        data.put(shape.unitsKey(), merged);
        detail.setData(data);
        return actions;
    }

    /**
     * Preserve dual Loose Flap + Inhibition sections on partial draft updates.
     */
    private void mergePostCureMotorSections(
            Map<String, Object> incoming,
            Map<String, Object> existing) {
        if (existing == null) {
            return;
        }
        for (String key : List.of(
                "looseFlapFillingDetails",
                "inhibitionDetails",
                "inhibitorType")) {
            if (!incoming.containsKey(key) && existing.containsKey(key)) {
                incoming.put(key, existing.get(key));
                continue;
            }
            // Treat empty {} shells as absent so draft partials do not wipe rich nested data.
            if (("looseFlapFillingDetails".equals(key) || "inhibitionDetails".equals(key))
                    && isEmptyMapShell(incoming.get(key))
                    && existing.get(key) instanceof Map<?, ?> existingMap
                    && !existingMap.isEmpty()) {
                incoming.put(key, existing.get(key));
            }
        }
    }

    private boolean isEmptyMapShell(Object value) {
        return value instanceof Map<?, ?> map && map.isEmpty();
    }

    private void normalizeTrimmingMotorPayload(Map<String, Object> motor) {
        for (String field : List.of(
                "trimmingOperationDetails", "trimmingMeasurementDetails")) {
            if (motor.containsKey(field)) {
                motor.put(field, copiedMapList(
                        motor.get(field), field + " must be an array of objects"));
            }
        }
        if (motor.containsKey("trimmingRemarks")) {
            motor.put("trimmingRemarks", new LinkedHashMap<>(mapValue(
                    motor.get("trimmingRemarks"),
                    "trimmingRemarks must be an object")));
        }
    }

    private DivisionDetail getOrCreateDetail(
            QcDivision form,
            QcDivisionWorkflow workflow,
            String storedDivision,
            String subType,
            String unitsKey) {
        return findDetail(form, workflow.getDivision(), subType).orElseGet(() -> {
            Map<String, Object> data = new LinkedHashMap<>();
            if (unitsKey != null) data.put(unitsKey, new ArrayList<>());
            DivisionDetail created = DivisionDetail.builder()
                    .divisionId(workflow.getDivisionId())
                    .division(storedDivision)
                    .subType(subType)
                    .status(Status.IN_PROGRESS)
                    .data(data)
                    .build();
            form.getDivisionDetails().add(created);
            return created;
        });
    }

    private Optional<DivisionDetail> findDetail(QcDivision form, String division, String subType) {
        String canonical = canonicalDivision(division);
        Optional<DivisionDetail> found = Optional.ofNullable(form.getDivisionDetails())
                .orElseGet(Collections::emptyList).stream()
                .filter(detail -> Objects.equals(effectiveDivision(detail), canonical))
                .filter(detail -> detailSubTypeMatches(detail, canonical, subType))
                .findFirst();
        found.ifPresent(detail -> {
            if (!Objects.equals(detail.getDivision(), canonical)
                    && !"RAW_MATERIAL".equals(detail.getDivision())) {
                detail.setDivision(canonical);
            }
        });
        return found;
    }

    private Optional<Map<String, Object>> findUnit(
            DivisionDetail detail,
            String unitsKey,
            String identityKey,
            Object identity,
            String stageType) {
        return units(detail, unitsKey).stream()
                .filter(unit -> Objects.equals(normalizedIdentity(unit.get(identityKey)), normalizedIdentity(identity)))
                .filter(unit -> stageType == null
                        || Objects.equals(normalizeNullable(stringValue(unit.get("stageType"))), stageType))
                .findFirst();
    }

    private Object normalizedIdentity(Object value) {
        return value instanceof Number number ? number.intValue() : stringValue(value);
    }

    private List<Map<String, Object>> units(DivisionDetail detail, String unitsKey) {
        if (detail == null || detail.getData() == null) return Collections.emptyList();
        Object value = detail.getData().get(unitsKey);
        if (!(value instanceof Collection<?> collection)) return Collections.emptyList();
        return collection.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private QcDivisionWorkflow workflow(QcDivision form, String division) {
        String canonical = canonicalDivision(division);
        QcDivisionWorkflow found = form.getDivisionWorkflow().stream()
                .filter(item -> Objects.equals(
                        QcDivisionCatalog.normalizeDivisionName(item.getDivision()),
                        canonical))
                .findFirst()
                .orElseThrow(() -> new AcemException(ErrorCode.INVALID_REQUEST,
                        "Unsupported QC division: " + division));
        // Heal legacy PROPELLANT_PROPERTIES rows so UnitShape / status keys stay on QC.
        if (!Objects.equals(found.getDivision(), canonical)) {
            found.setDivision(canonical);
        }
        return found;
    }

    private void requireWorkflowUnlocked(QcDivisionWorkflow workflow) {
        if (workflow.isLocked() || workflow.getStatus() == Status.YET_TO_START) {
            throw new AcemException(ErrorCode.DIVISION_LOCKED,
                    "QC division is locked: " + workflow.getDivision());
        }
    }

    private void requireUnlocked(QcDivision form, QcDivisionWorkflow workflow) {
        requireWorkflowUnlocked(workflow);
        batchRepo.findByBatchIdAndIsDeletedFalse(form.getBatchId()).ifPresent(batch -> mainBatchGateValidator.assertQcDivisionUnlocked(batch, workflow.getDivision()));
    }

    private Status optionalSubmissionStatus(Object value, String field) {
        if (value == null || stringValue(value).isBlank()) return null;
        return submissionStatus(value, field);
    }

    private Status statusFor(Status submissionType) {
        return submissionType == Status.DRAFT ? Status.IN_PROGRESS : Status.WAITING_FOR_APPROVAL;
    }

    private Integer validatePremixNo(Batch batch, Object value) {
        Integer premixNo = integerValue(value);
        int totalPremixes = batch.getIdentificationSheet() != null
                && batch.getIdentificationSheet().getNumberOfPremix() != null
                ? batch.getIdentificationSheet().getNumberOfPremix() : 0;
        if (premixNo == null || premixNo < 1 || premixNo > totalPremixes) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "premixNo must be between 1 and " + totalPremixes);
        }
        return premixNo;
    }

    private void validateMotorId(
            Batch batch,
            String motorId,
            String identityKey,
            String motorDivision) {
        if (motorId == null || motorId.isBlank()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, identityKey + " is required");
        }
        if (batch.getMotorIds() != null && batch.getMotorIds().contains(motorId)) {
            return;
        }
        // STF QC accepts BEM mould IDs (bemNo) which are not on batch.motorIds.
        if (QcDivisionCatalog.STATIC_TEST_FACILITY.equals(motorDivision)
                && belongsToBatchAsStfMotor(batch, motorId)) {
            return;
        }
        throw new AcemException(ErrorCode.INVALID_REQUEST,
                "Motor " + motorId + " does not belong to batch");
    }

    /** MAIN motors from batch.motorIds plus BEM units seeded on manufacturing STF / subscale. */
    private boolean belongsToBatchAsStfMotor(Batch batch, String motorId) {
        boolean onStfStage = MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.STF)
                .map(Batch.StageProgress::getMotorStatuses)
                .orElseGet(List::of)
                .stream()
                .anyMatch(status -> status != null && motorId.equals(status.getMotorId()));
        if (onStfStage) {
            return true;
        }
        SubScaleMetadata metadata =
                subscaleMetadataResolver.resolveApprovedMetadata(batch.getBatchId());
        return subscaleMetadataResolver.toBemMotorIds(metadata).contains(motorId);
    }

    private List<Map<String, Object>> copiedMapList(Object value, String message) {
        return mapList(value, message).stream()
                .map(item -> (Map<String, Object>) new LinkedHashMap<>(item))
                .toList();
    }

    private List<Map<String, Object>> optionalCopiedMapList(
            Object value,
            String field) {
        if (value == null) return Collections.emptyList();
        return copiedMapList(value, field + " must be an array of objects");
    }

    private Map<String, Object> mapCopy(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return copy;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private List<Map<String, Object>> mergeUntouchedData(
            DivisionDetail detail, String unitsKey) {
        return new ArrayList<>(units(detail, unitsKey));
    }

    private Map<String, Object> mergeData(
            DivisionDetail detail, Map<String, Object> incomingData) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (detail.getData() != null) merged.putAll(detail.getData());
        merged.putAll(incomingData);
        return merged;
    }

    private void replaceUnit(
            List<Map<String, Object>> values,
            Map<String, Object> incoming,
            java.util.function.Predicate<Map<String, Object>> identity) {
        values.removeIf(identity);
        values.add(incoming);
    }

    private void retainUnitWorkflowFields(
            Map<String, Object> incoming, Map<String, Object> existing) {
        if (existing == null) return;
        for (String key : List.of("premixSubmissionType", "premixSubmissionStatus",
                "motorSubmissionType", "motorSubmissionStatus", "remarks",
                "rejectionReason", "submittedBy", "submittedAt", "approvedBy", "approvedAt",
                "actionBy", "actionAt")) {
            if (!incoming.containsKey(key) && existing.containsKey(key)) {
                incoming.put(key, existing.get(key));
            }
        }
    }

    private boolean sameWorkflowState(
            Map<String, Object> incoming,
            String typeKey,
            String statusKey,
            Map<String, Object> existing) {
        return existing != null && sameWorkflowState(incoming, typeKey, statusKey,
                readStatus(existing.get(typeKey)), readStatus(existing.get(statusKey)));
    }

    private boolean sameWorkflowState(
            Map<String, Object> incoming,
            String typeKey,
            String statusKey,
            Status existingType,
            Status existingStatus) {
        Status incomingStatus = readStatus(incoming.get(statusKey));
        return incomingStatus != null
                && readStatus(incoming.get(typeKey)) == existingType
                && incomingStatus == existingStatus;
    }

    private void ensureProtectedRoundTripUnchanged(
            boolean roundTrip,
            Map<String, Object> incoming,
            Map<String, Object> existing,
            String unitName,
            String statusKey) {
        Status existingStatus = existing == null ? null : readStatus(existing.get(statusKey));
        if (roundTrip && isApprovalProtected(existingStatus)
                && !Objects.equals(businessFields(incoming), businessFields(existing))) {
            requireEditable(existingStatus, unitName);
        }
    }

    private boolean isApprovalProtected(Status status) {
        return status == Status.WAITING_FOR_APPROVAL || status == Status.APPROVED;
    }

    private Map<String, Object> businessFields(Map<String, Object> source) {
        if (source == null) return Collections.emptyMap();
        Map<String, Object> values = new LinkedHashMap<>(source);
        for (String key : List.of("premixSubmissionType", "premixSubmissionStatus",
                "motorSubmissionType", "motorSubmissionStatus", "remarks",
                "rejectionReason", "submittedBy", "submittedAt", "approvedBy", "approvedAt",
                "actionBy", "actionAt")) {
            values.remove(key);
        }
        return values;
    }

    private Map<String, Object> nestedMap(Map<String, Object> parent, String key) {
        if (parent == null || !(parent.get(key) instanceof Map<?, ?> value)) return null;
        return (Map<String, Object>) value;
    }

    private String mixingDetailsKey(String stageType) {
        return "PREMIX".equals(stageType) ? "premixDetails" : "finalMixDetails";
    }

    private Status submissionStatus(Object value, String field) {
        Status status = readStatus(value);
        if (status != Status.DRAFT && status != Status.SUBMIT) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    field + " must be DRAFT or SUBMIT");
        }
        return status;
    }

    private Status readStatus(Object value) {
        if (value instanceof Status status) return status;
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Status.valueOf(text.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private void requireEditable(Status status, String unitName) {
        if (status == Status.WAITING_FOR_APPROVAL || status == Status.APPROVED) {
            throw new AcemException(ErrorCode.UNIT_NOT_EDITABLE,
                    unitName + " is " + status + " and cannot be updated");
        }
    }

    private void requireWaiting(Status status, String unitName) {
        if (status != Status.WAITING_FOR_APPROVAL) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    unitName + " is not waiting for approval");
        }
    }

    private void validateApprovalAction(Status action, String rejectionReason) {
        if (action != Status.APPROVED && action != Status.REJECTED) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Only APPROVED or REJECTED actions are allowed");
        }
        if (action == Status.REJECTED) {
            String trimmed = rejectionReason == null ? "" : rejectionReason.trim();
            if (trimmed.isEmpty()) {
                throw new AcemException(ErrorCode.REJECTION_REASON_REQUIRED,
                        "rejectionReason is required for rejection");
            }
            if (trimmed.length() < 3) {
                throw new AcemException(ErrorCode.REJECTION_REASON_REQUIRED,
                        "Rejection reason must be between 3 and 500 characters");
            }
        }
    }

    private void applyApproval(
            Map<String, Object> unit,
            String statusKey,
            Status action,
            String remarks,
            String rejectionReason,
            PersonInfo actor,
            LocalDateTime now) {
        unit.put(statusKey, action);
        unit.put("remarks", action == Status.APPROVED ? remarks : null);
        unit.put("rejectionReason", action == Status.REJECTED ? rejectionReason : null);
        unit.put("approvedBy", action == Status.APPROVED ? actor : null);
        unit.put("approvedAt", action == Status.APPROVED ? now : null);
        unit.remove("actionBy");
        unit.remove("actionAt");
    }

    private void clearUnitApproval(Map<String, Object> unit) {
        unit.put("remarks", null);
        unit.put("rejectionReason", null);
        unit.put("approvedBy", null);
        unit.put("approvedAt", null);
        unit.remove("actionBy");
        unit.remove("actionAt");
    }

    private void clearDivisionApproval(DivisionDetail detail) {
        detail.setRemarks(null);
        detail.setRejectionReason(null);
        clearDivisionApprovedAudit(detail);
    }

    private void clearDivisionApprovedAudit(DivisionDetail detail) {
        detail.setApprovedBy(null);
        detail.setApprovedAt(null);
        detail.setActionBy(null);
        detail.setActionAt(null);
    }

    private void applyDivisionApprovalAudit(
            DivisionDetail detail, PersonInfo actor, LocalDateTime now) {
        detail.setApprovedBy(actor);
        detail.setApprovedAt(now);
        detail.setActionBy(null);
        detail.setActionAt(null);
    }

    private void applySubmissionAudit(
            DivisionDetail detail,
            Status submissionType,
            PersonInfo actor,
            LocalDateTime now) {
        if (submissionType == Status.SUBMIT) {
            detail.setSubmittedBy(actor);
            detail.setSubmittedAt(now);
            detail.setDivisionSubmissionType(Status.SUBMIT);
            detail.setDivisionSubmissionStatus(statusFor(Status.SUBMIT));
        }
    }

    private void applySubmissionAudit(
            Map<String, Object> unit,
            Status submissionType,
            PersonInfo actor,
            LocalDateTime now) {
        if (submissionType == Status.SUBMIT) {
            unit.put("submittedBy", actor);
            unit.put("submittedAt", now);
        }
    }

    private String unlockSuccessor(
            QcDivision form,
            String division,
            PersonInfo actor,
            LocalDateTime now) {
        List<QcDivisionWorkflow> workflow = form.getDivisionWorkflow().stream()
                .sorted(Comparator.comparing(QcDivisionWorkflow::getSequence))
                .toList();
        String canonical = QcDivisionCatalog.normalizeDivisionName(division);
        for (int index = 0; index < workflow.size(); index++) {
            if (!Objects.equals(
                    QcDivisionCatalog.normalizeDivisionName(workflow.get(index).getDivision()),
                    canonical)) {
                continue;
            }
            if (index + 1 >= workflow.size()) return null;
            QcDivisionWorkflow next = workflow.get(index + 1);
            if (!next.isLocked()) return null;
            next.setLocked(false);
            next.setUnlockedBy(actor);
            next.setUnlockedAt(now);
            return QcDivisionCatalog.normalizeDivisionName(next.getDivision());
        }
        return null;
    }

    private Aggregation aggregate(QcDivision form, Batch batch) {
        List<QcMotorStatusDto> motorStatuses = new ArrayList<>();
        List<QcPremixStatusDto> premixStatuses = new ArrayList<>();
        List<DivisionStatusDto> divisionStatuses = new ArrayList<>();
        List<QcDivisionWorkflow> workflow = Optional.ofNullable(form.getDivisionWorkflow())
                .orElseGet(Collections::emptyList).stream()
                .sorted(Comparator.comparing(QcDivisionWorkflow::getSequence))
                .toList();

        for (QcDivisionWorkflow item : workflow) {
            String division = canonicalDivision(item.getDivision());
            List<DivisionDetail> details = Optional.ofNullable(form.getDivisionDetails())
                    .orElseGet(Collections::emptyList).stream()
                    .filter(detail -> Objects.equals(effectiveDivision(detail), division))
                    .toList();

            List<Status> unitStatuses = new ArrayList<>();
            if (DIVISION_SCOPED_DIVISIONS.contains(division)) {
                Status status;
                if (item.isLocked()) {
                    status = Status.YET_TO_START;
                } else if (details.isEmpty() || details.getFirst().getDivisionSubmissionStatus() == null) {
                    status = Status.TO_BE_INITIATED;
                } else {
                    status = details.getFirst().getDivisionSubmissionStatus();
                }
                unitStatuses.add(status);
            } else if (PREMIX_DIVISIONS.contains(division)) {
                buildPremixStatuses(division, details, batch, premixStatuses, unitStatuses, item.isLocked());
            } else {
                buildMotorStatuses(division, details, batch, motorStatuses, unitStatuses, item.isLocked());
            }

            int total = unitStatuses.size();
            int approved = count(unitStatuses, Status.APPROVED);
            int rejected = count(unitStatuses, Status.REJECTED);
            int waiting = count(unitStatuses, Status.WAITING_FOR_APPROVAL);
            int inProgress = count(unitStatuses, Status.IN_PROGRESS);
            int started = approved + rejected + waiting + inProgress;
            Status divisionStatus;
            // Locked catalog rows must stay YET_TO_START — TO_BE_INITIATED is the unlock signal
            // for QC UI tabs (NDT via mfg NDT APPROVED; STF/QC/WEIGHTMENT via STF BEM APPROVED).
            if (item.isLocked()) divisionStatus = Status.YET_TO_START;
            else if (rejected > 0) divisionStatus = Status.REJECTED;
            else if (total > 0 && approved == total) divisionStatus = Status.APPROVED;
            else if (waiting > 0) divisionStatus = Status.WAITING_FOR_PARTIAL_APPROVAL;
            else if (started > 0) divisionStatus = Status.IN_PROGRESS;
            else divisionStatus = Status.TO_BE_INITIATED;

            item.setStatus(divisionStatus);
            details.forEach(detail -> detail.setStatus(divisionStatus));
            DivisionDetail submissionAudit = latestAudit(details, DivisionDetail::getSubmittedAt);
            DivisionDetail approvalAudit = latestAudit(details, this::divisionApprovedAt);
            divisionStatuses.add(DivisionStatusDto.builder()
                    .divisionId(item.getDivisionId())
                    .division(division)
                    .sequence(item.getSequence())
                    .unlockedBy(item.getUnlockedBy())
                    .unlockedAt(item.getUnlockedAt())
                    .submittedBy(submissionAudit == null ? null : submissionAudit.getSubmittedBy())
                    .submittedAt(submissionAudit == null ? null : submissionAudit.getSubmittedAt())
                    .approvedBy(approvalAudit == null ? null : divisionApprovedBy(approvalAudit))
                    .approvedAt(approvalAudit == null ? null : divisionApprovedAt(approvalAudit))
                    .status(divisionStatus)
                    .allUnitsApproved(total > 0 && approved == total)
                    .pendingUnitCount(waiting)
                    .approvedUnitCount(approved)
                    .rejectedUnitCount(rejected)
                    .waitingForApprovalUnitCount(waiting)
                    .inProgressUnitCount(inProgress)
                    .totalUnitCount(total)
                    .build());
        }

        int approvedDivisions = (int) divisionStatuses.stream()
                .filter(status -> status.getStatus() == Status.APPROVED).count();
        int totalDivisions = divisionStatuses.size();
        int pendingMotors = (int) motorStatuses.stream()
                .filter(status -> status.getMotorSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
        int approvedMotors = (int) motorStatuses.stream()
                .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED).count();
        int rejectedMotors = (int) motorStatuses.stream()
                .filter(status -> status.getMotorSubmissionStatus() == Status.REJECTED).count();
        int inProgressMotors = (int) motorStatuses.stream()
                .filter(status -> status.getMotorSubmissionStatus() == Status.IN_PROGRESS).count();
        int pendingPremixes = (int) premixStatuses.stream()
                .filter(status -> status.getPremixSubmissionStatus() == Status.WAITING_FOR_APPROVAL).count();
        int approvedPremixes = (int) premixStatuses.stream()
                .filter(status -> status.getPremixSubmissionStatus() == Status.APPROVED).count();
        int rejectedPremixes = (int) premixStatuses.stream()
                .filter(status -> status.getPremixSubmissionStatus() == Status.REJECTED).count();

        return new Aggregation(
                divisionStatuses,
                motorStatuses,
                premixStatuses,
                totalDivisions > 0 && approvedDivisions == totalDivisions,
                approvedDivisions,
                totalDivisions,
                !motorStatuses.isEmpty() && approvedMotors == motorStatuses.size(),
                pendingMotors,
                approvedMotors,
                rejectedMotors,
                inProgressMotors,
                motorStatuses.size(),
                !premixStatuses.isEmpty() && approvedPremixes == premixStatuses.size(),
                pendingPremixes,
                approvedPremixes,
                rejectedPremixes,
                premixStatuses.size());
    }

    private void buildMotorStatuses(
            String division,
            List<DivisionDetail> details,
            Batch batch,
            List<QcMotorStatusDto> target,
            List<Status> statuses,
            boolean locked) {
        UnitShape shape = MOTOR_UNIT_SHAPES.get(division);
        List<String> subTypes = details.stream()
                .map(DivisionDetail::getSubType)
                .map(this::normalizeNullable)
                .distinct().toList();
        if (subTypes.isEmpty()) subTypes = Collections.singletonList(null);
        List<String> motorIds = Optional.ofNullable(batch.getMotorIds()).orElseGet(Collections::emptyList);
        boolean useUnitLocks = usesQcUnitLocks(batch);
        boolean stfDivision = QcDivisionCatalog.STATIC_TEST_FACILITY.equals(division);
        for (String subType : subTypes) {
            DivisionDetail detail = details.stream()
                    .filter(value -> Objects.equals(normalizeNullable(value.getSubType()), subType))
                    .findFirst().orElse(null);
            Map<String, Map<String, Object>> saved = detail == null ? Collections.emptyMap()
                    : units(detail, shape.unitsKey()).stream()
                    .filter(unit -> stringValue(unit.get(shape.identityKey())) != null)
                    .collect(Collectors.toMap(
                            unit -> stringValue(unit.get(shape.identityKey())),
                            Function.identity(), (left, right) -> right, LinkedHashMap::new));
            // STF roster includes BEM mould IDs — batch.motorIds alone is MAIN-only.
            List<String> roster = stfDivision
                    ? stfMotorRoster(batch, saved)
                    : (motorIds.isEmpty() ? new ArrayList<>(saved.keySet()) : motorIds);
            for (String motorId : roster) {
                Map<String, Object> unit = saved.get(motorId);
                boolean unitUnlocked = !locked && (!useUnitLocks
                        || QcDivisionProgressHelper.isMotorUnitUnlocked(batch, division, motorId));
                Status missingUnitStatus = unitUnlocked ? Status.TO_BE_INITIATED : Status.YET_TO_START;
                Status status = unit == null ? missingUnitStatus
                        : Optional.ofNullable(readStatus(unit.get("motorSubmissionStatus")))
                        .orElse(missingUnitStatus);
                // Locked division / unit must not report unlocked unit rows into stageProgress.
                if (!unitUnlocked && (status == Status.TO_BE_INITIATED || status == Status.YET_TO_START)) {
                    status = Status.YET_TO_START;
                }
                String resolvedSubType = unit == null
                        ? subType
                        : Optional.ofNullable(normalizeNullable(stringValue(unit.get("subType"))))
                                .orElse(subType);
                statuses.add(status);
                target.add(QcMotorStatusDto.builder()
                        .division(division)
                        .subType(resolvedSubType)
                        .motorId(motorId)
                        .motorSubmissionType(unit == null ? null : readStatus(unit.get("motorSubmissionType")))
                        .motorSubmissionStatus(status)
                        .remarks(unit == null ? null : stringValue(unit.get("remarks")))
                        .rejectionReason(unit == null ? null : stringValue(unit.get("rejectionReason")))
                        .submittedBy(unit == null ? null : personValue(unit.get("submittedBy")))
                        .submittedAt(unit == null ? null : dateTimeValue(unit.get("submittedAt")))
                        .approvedBy(unit == null ? null : approvedBy(unit, status))
                        .approvedAt(unit == null ? null : approvedAt(unit, status))
                        .build());
            }
        }
    }


    private List<String> stfMotorRoster(Batch batch, Map<String, Map<String, Object>> saved) {
        LinkedHashSet<String> roster = new LinkedHashSet<>();
        MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.STF)
                .map(Batch.StageProgress::getMotorStatuses)
                .orElseGet(List::of)
                .stream()
                .map(MotorStatusResponse::getMotorId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .forEach(roster::add);
        if (roster.isEmpty()) {
            Optional.ofNullable(batch.getMotorIds()).orElseGet(List::of).stream()
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isBlank())
                    .forEach(roster::add);
            SubScaleMetadata metadata =
                    subscaleMetadataResolver.resolveApprovedMetadata(batch.getBatchId());
            roster.addAll(subscaleMetadataResolver.toBemMotorIds(metadata));
        }
        if (saved != null) {
            roster.addAll(saved.keySet());
        }
        return new ArrayList<>(roster);
    }

    private void buildPremixStatuses(
            String division,
            List<DivisionDetail> details,
            Batch batch,
            List<QcPremixStatusDto> target,
            List<Status> statuses,
            boolean locked) {
        int totalPremixes = batch.getIdentificationSheet() != null
                && batch.getIdentificationSheet().getNumberOfPremix() != null
                ? batch.getIdentificationSheet().getNumberOfPremix() : 0;
        if ("MIXING".equals(division)) {
            for (String stageType : List.of("PREMIX", "FINAL_MIX")) {
                Map<Integer, Map<String, Object>> saved = indexMixingStageUnits(details, stageType);
                for (int premixNo = 1; premixNo <= totalPremixes; premixNo++) {
                    Map<String, Object> unit = saved.get(premixNo);
                    addPremixStatus(
                            target, statuses, division, null, stageType, premixNo, unit, locked, batch);
                }
            }
            return;
        }
        List<String> subTypes = details.stream()
                .map(DivisionDetail::getSubType)
                .map(this::normalizeNullable)
                .distinct().toList();
        if (subTypes.isEmpty()) subTypes = Collections.singletonList(null);
        for (String subType : subTypes) {
            DivisionDetail detail = details.stream()
                    .filter(value -> Objects.equals(normalizeNullable(value.getSubType()), subType))
                    .findFirst().orElse(null);
            Map<Integer, Map<String, Object>> saved = premixUnitsByNumber(detail);
            for (int premixNo = 1; premixNo <= totalPremixes; premixNo++) {
                Map<String, Object> unit = saved.get(premixNo);
                addPremixStatus(
                        target, statuses, division, subType, null, premixNo, unit, locked, batch);
            }
        }
    }

    private Map<Integer, Map<String, Object>> indexMixingStageUnits(
            List<DivisionDetail> details,
            String stageType) {
        String detailsKey = mixingDetailsKey(stageType);
        Map<Integer, Map<String, Object>> stageUnits = new LinkedHashMap<>();

        Optional.ofNullable(details).orElseGet(Collections::emptyList).stream()
                .sorted(Comparator.comparingInt(detail -> mixingDetailPriority(detail, stageType)))
                .forEach(detail -> premixUnitsByNumber(detail).forEach((premixNo, premix) -> {
                    Map<String, Object> stage = nestedMap(premix, detailsKey);
                    if (stage != null) {
                        stageUnits.putIfAbsent(premixNo, stage);
                    }
                }));
        return stageUnits;
    }

    private MixingApprovalTarget findMixingApprovalTarget(
            QcDivision form,
            String requestedSubType,
            String stageType,
            Integer premixNo) {
        List<DivisionDetail> mixingDetails = Optional.ofNullable(form.getDivisionDetails())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(detail -> "MIXING".equals(effectiveDivision(detail)))
                .filter(detail -> requestedSubType == null
                        || detailSubTypeMatches(detail, "MIXING", requestedSubType))
                .sorted(Comparator.comparingInt(detail -> mixingDetailPriority(detail, stageType)))
                .toList();

        if (mixingDetails.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "QC premix division data not found");
        }

        boolean premixFound = false;
        String detailsKey = mixingDetailsKey(stageType);
        for (DivisionDetail detail : mixingDetails) {
            Map<String, Object> premixContainer = findUnit(
                    detail, "premixes", "premixNo", premixNo, null).orElse(null);
            if (premixContainer == null) continue;
            premixFound = true;
            Map<String, Object> stageDetails = nestedMap(premixContainer, detailsKey);
            if (stageDetails != null) {
                return new MixingApprovalTarget(detail, stageDetails);
            }
        }

        if (!premixFound) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "Premix not found");
        }
        throw new AcemException(ErrorCode.INVALID_REQUEST,
                stageType + " details not found for premix " + premixNo);
    }

    private int mixingDetailPriority(DivisionDetail detail, String stageType) {
        String subType = normalizeNullable(detail.getSubType());
        if (Objects.equals(subType, stageType)) return 0;
        if (subType == null) return 1;
        return 2;
    }

    private Map<Integer, Map<String, Object>> premixUnitsByNumber(DivisionDetail detail) {
        if (detail == null) return Collections.emptyMap();
        return units(detail, "premixes").stream()
                .filter(unit -> integerValue(unit.get("premixNo")) != null)
                .collect(Collectors.toMap(
                        unit -> integerValue(unit.get("premixNo")),
                        Function.identity(), (left, right) -> right, LinkedHashMap::new));
    }

    private void addPremixStatus(
            List<QcPremixStatusDto> target,
            List<Status> statuses,
            String division,
            String subType,
            String stageType,
            int premixNo,
            Map<String, Object> unit,
            boolean locked,
            Batch batch) {
        StageType stageEnum = "FINAL_MIX".equals(stageType) ? StageType.FINAL_MIX : StageType.PREMIX;
        boolean unitUnlocked = !locked && (!usesQcUnitLocks(batch)
                || QcDivisionProgressHelper.isPremixUnitUnlocked(
                batch, division, premixNo, stageEnum));
        Status missingUnitStatus = unitUnlocked ? Status.TO_BE_INITIATED : Status.YET_TO_START;
        Status status = unit == null ? missingUnitStatus
                : Optional.ofNullable(readStatus(unit.get("premixSubmissionStatus")))
                .orElse(missingUnitStatus);
        // One approved Premix must not unlock sibling Premix / Final Mix rows.
        if (!unitUnlocked && (status == Status.TO_BE_INITIATED || status == Status.YET_TO_START)) {
            status = Status.YET_TO_START;
        }
        statuses.add(status);
        target.add(QcPremixStatusDto.builder()
                .division(division)
                .subType(subType)
                .stageType(stageType)
                .premixNo(premixNo)
                .premixSubmissionType(unit == null ? null : readStatus(unit.get("premixSubmissionType")))
                .premixSubmissionStatus(status)
                .remarks(unit == null ? null : stringValue(unit.get("remarks")))
                .rejectionReason(unit == null ? null : stringValue(unit.get("rejectionReason")))
                .submittedBy(unit == null ? null : personValue(unit.get("submittedBy")))
                .submittedAt(unit == null ? null : dateTimeValue(unit.get("submittedAt")))
                .approvedBy(unit == null ? null : approvedBy(unit, status))
                .approvedAt(unit == null ? null : approvedAt(unit, status))
                .build());
    }

    private static boolean usesQcUnitLocks(Batch batch) {
        return MainBatchStageProgressHelper.isParallelMainFlow(batch)
                || QcDivisionProgressHelper.isQualificationSubscale(batch)
                || QcDivisionProgressHelper.isExperimentalSubscale(batch);
    }

    private boolean divisionApproved(Aggregation aggregation, String division) {
        return aggregation.divisionStatuses().stream()
                .anyMatch(status -> Objects.equals(status.getDivision(), division)
                        && status.getStatus() == Status.APPROVED);
    }

    private DivisionDetail latestAudit(
            List<DivisionDetail> details,
            Function<DivisionDetail, LocalDateTime> timestamp) {
        return details.stream()
                .filter(detail -> timestamp.apply(detail) != null)
                .max(Comparator.comparing(timestamp))
                .orElse(null);
    }

    private PersonInfo divisionApprovedBy(DivisionDetail detail) {
        return detail.getApprovedBy() != null ? detail.getApprovedBy() : detail.getActionBy();
    }

    private LocalDateTime divisionApprovedAt(DivisionDetail detail) {
        return detail.getApprovedAt() != null ? detail.getApprovedAt() : detail.getActionAt();
    }

    private PersonInfo approvedBy(Map<String, Object> unit, Status status) {
        PersonInfo approvedBy = personValue(unit.get("approvedBy"));
        return approvedBy != null || status != Status.APPROVED
                ? approvedBy : personValue(unit.get("actionBy"));
    }

    private LocalDateTime approvedAt(Map<String, Object> unit, Status status) {
        LocalDateTime approvedAt = dateTimeValue(unit.get("approvedAt"));
        return approvedAt != null || status != Status.APPROVED
                ? approvedAt : dateTimeValue(unit.get("actionAt"));
    }

    private int count(List<Status> statuses, Status expected) {
        return (int) statuses.stream().filter(status -> status == expected).count();
    }

    private void applyAggregateStatus(QcDivision form, Aggregation aggregation) {
        this.finalSubmit = false;

        if (aggregation.allDivisionsApproved()) {
            form.setStatus(Status.COMPLETELY_APPROVED);
            return;
        }
        boolean rejected = aggregation.divisionStatuses().stream()
                .anyMatch(status -> status.getStatus() == Status.REJECTED);
        boolean waiting = aggregation.divisionStatuses().stream()
                .anyMatch(status -> status.getStatus() == Status.WAITING_FOR_PARTIAL_APPROVAL);
        form.setStatus(rejected ? Status.REJECTED
                : waiting ? Status.WAITING_FOR_PARTIAL_APPROVAL : Status.IN_PROGRESS);
    }

    private void applyAutomaticCompletion(
            QcDivision form,
            Aggregation aggregation,
            PersonInfo actor,
            LocalDateTime now) {
        if (!aggregation.allDivisionsApproved()) {
            return;
        }
        form.setFormSubmissionType(Status.SUBMIT);
        form.setStatus(Status.COMPLETELY_APPROVED);
        form.setSubmittedBy(actor);
        form.setSubmittedOn(now);
        form.setApprovedBy(actor);
        form.setApprovedOn(now);
        form.setRejectionReason(null);
    }

    private List<Map<String, Object>> buildPendingDivisionDetails(Aggregation aggregation) {
        return aggregation.divisionStatuses().stream()
                .filter(status -> status.getStatus() != Status.APPROVED)
                .map(status -> {
                    Map<String, Object> pending = new LinkedHashMap<>();
                    pending.put("division", status.getDivision());
                    pending.put("status", status.getStatus());
                    pending.put("approvedUnitCount", status.getApprovedUnitCount());
                    pending.put("totalUnitCount", status.getTotalUnitCount());
                    return pending;
                }).toList();
    }

    private QcDivisionFormResponse buildResponse(
            QcDivision form,
            Batch batch,
            Aggregation aggregation,
            ActionContext action,
            boolean includeDetails) {
        Map<String, QualityCheckParams> mixingQualityChecksById = includeDetails
                ? loadDivisionMixingQualityChecksById(form)
                : Collections.emptyMap();
        List<DivisionDetailDto> details = includeDetails
                ? Optional.ofNullable(form.getDivisionDetails()).orElseGet(Collections::emptyList).stream()
                .map(detail -> DivisionDetailDto.builder()
                        .divisionId(detail.getDivisionId())
                        .division(detail.getDivision())
                        .subType(detail.getSubType())
                        .divisionSubmissionType(detail.getDivisionSubmissionType())
                        .divisionSubmissionStatus(detail.getDivisionSubmissionStatus())
                        .status(detail.getStatus())
                        .remarks(detail.getRemarks())
                        .rejectionReason(detail.getRejectionReason())
                        .actionBy(divisionApprovedBy(detail))
                        .actionAt(divisionApprovedAt(detail))
                        .submittedBy(detail.getSubmittedBy())
                        .submittedAt(detail.getSubmittedAt())
                        .approvedBy(detail.getApprovedBy())
                        .approvedAt(detail.getApprovedAt())
                        .data(toEnrichedDivisionDataResponse(
                                detail,
                                enrichMixingDivisionData(detail, mixingQualityChecksById),
                                form.getSubDepartmentId()))
                        .build()).toList()
                : null;
        return QcDivisionFormResponse.builder()
                .formId(form.getFormId())
                .batchId(form.getBatchId())
                .subDepartmentId(form.getSubDepartmentId())
                .formSubmissionType(form.getFormSubmissionType())
                .status(form.getStatus())
                .divisionStatuses(aggregation.divisionStatuses())
                .motorStatuses(aggregation.motorStatuses())
                .premixStatuses(qcPremixStatuses(aggregation))
                .finalMixStatuses(qcFinalMixStatuses(aggregation))
                .divisionDetails(details)
                .createdBy(form.getCreatedBy())
                .createdAt(form.getCreatedAt())
                .submittedBy(form.getSubmittedBy())
                .submittedAt(form.getSubmittedOn())
                .approvedBy(form.getApprovedBy())
                .approvedAt(form.getApprovedOn())
                .build();
    }

    private Map<String, QualityCheckParams> loadDivisionMixingQualityChecksById(
            QcDivision form) {
        Set<String> parameterIds = new LinkedHashSet<>();
        Optional.ofNullable(form.getDivisionDetails())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(detail -> "MIXING".equals(effectiveDivision(detail)))
                .map(DivisionDetail::getData)
                .forEach(data -> collectQualityCheckParameterIds(data, parameterIds));
        return loadQualityChecksByIds(parameterIds);
    }

    private void collectQualityCheckParameterIds(
            Object value,
            Set<String> parameterIds) {
        if (value instanceof QualityCheckParameterDto parameter) {
            if (parameter.getParameterId() != null) {
                parameterIds.add(parameter.getParameterId());
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            String parameterId = stringValue(map.get("parameterId"));
            if (parameterId != null && !parameterId.isBlank()) {
                parameterIds.add(parameterId);
            }
            map.values().forEach(item -> collectQualityCheckParameterIds(item, parameterIds));
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectQualityCheckParameterIds(item, parameterIds));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enrichMixingDivisionData(
            DivisionDetail detail,
            Map<String, QualityCheckParams> qualityChecksById) {
        if (!"MIXING".equals(effectiveDivision(detail)) || detail.getData() == null) {
            return detail.getData();
        }
        return (Map<String, Object>) enrichQualityCheckMetadata(
                detail.getData(), qualityChecksById);
    }

    private QcDivisionPayloads.DivisionDataResponse toEnrichedDivisionDataResponse(
            DivisionDetail detail,
            Map<String, Object> data,
            Long subDepartmentId) {
        Map<String, Object> normalized = normalizeDivisionWireData(detail, data);
        Map<String, Object> fileEnriched = enrichDivisionFileMetadata(
                normalized,
                detail.getDivision(),
                detail.getSubType(),
                subDepartmentId);
        return qcDivisionPayloadMapper.toResponse(
                detail.getDivision(),
                detail.getSubType(),
                fileEnriched);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enrichDivisionFileMetadata(
            Map<String, Object> data,
            String division,
            String subType,
            Long qcSubDepartmentId) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        JsonNode rootNode = objectMapper.valueToTree(data);
        QcFileScopeContext scopeContext = new QcFileScopeContext(
                null,
                resolvePayloadDivision(division, subType));
        enrichDivisionFileMetadataNode(rootNode, qcSubDepartmentId, scopeContext);
        return objectMapper.convertValue(rootNode, Map.class);
    }

    private void enrichDivisionFileMetadataNode(
            JsonNode node,
            Long qcSubDepartmentId,
            QcFileScopeContext scopeContext) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            QcFileScopeContext nodeContext = scopeContext.withNode(objectNode);

            if (objectNode.hasNonNull("fileId")) {
                String fileId = objectNode.get("fileId").asText().trim();
                if (!fileId.isEmpty()) {
                    fileMetadataEnrichmentService.enrichJsonFileNodes(
                            objectNode,
                            nodeContext.sourceSubDepartmentId(),
                            nodeContext.motorId());
                    fileAttachmentLookupService.resolve(fileId, null, nodeContext.motorId())
                            .ifPresent(attachment -> {
                                applyAttachmentMetadata(objectNode, attachment);
                                if (!Objects.equals(attachment.getSubDepartmentId(), qcSubDepartmentId)) {
                                    objectNode.put("readOnly", true);
                                }
                            });
                }
            }

            objectNode.fields().forEachRemaining(entry ->
                    enrichDivisionFileMetadataNode(entry.getValue(), qcSubDepartmentId, nodeContext));
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                enrichDivisionFileMetadataNode(element, qcSubDepartmentId, scopeContext);
            }
        }
    }

    private Map<String, Object> normalizeDivisionWireData(
            DivisionDetail detail,
            Map<String, Object> data) {
        Map<String, Object> normalized = normalizeRevalidationWireData(detail, data);
        return normalizeHardwareWireData(detail, normalized);
    }

    private Map<String, Object> normalizeRevalidationWireData(
            DivisionDetail detail,
            Map<String, Object> data) {
        if (data == null || !"RAW_MATERIAL_REVALIDATION".equals(effectiveDivision(detail))) {
            return data;
        }
        Map<String, Object> copy = new LinkedHashMap<>(data);
        Object materialsNode = copy.get("materials");
        if (!(materialsNode instanceof List<?> materials)) {
            return copy;
        }
        List<Object> normalizedMaterials = new ArrayList<>();
        for (Object materialObj : materials) {
            if (!(materialObj instanceof Map<?, ?> materialMap)) {
                normalizedMaterials.add(materialObj);
                continue;
            }
            Map<String, Object> material = new LinkedHashMap<>();
            materialMap.forEach((k, v) -> material.put(String.valueOf(k), v));
            Object qcDetailsNode = material.get("qcDetails");
            if (qcDetailsNode instanceof List<?> qcDetails) {
                List<Object> normalizedDetails = new ArrayList<>();
                for (Object detailObj : qcDetails) {
                    if (!(detailObj instanceof Map<?, ?> detailMap)) {
                        normalizedDetails.add(detailObj);
                        continue;
                    }
                    Map<String, Object> qcDetail = new LinkedHashMap<>();
                    detailMap.forEach((k, v) -> qcDetail.put(String.valueOf(k), v));
                    Object acemQcResult = qcDetail.containsKey("acemQcResult")
                            ? qcDetail.get("acemQcResult")
                            : qcDetail.get("acemResult");
                    qcDetail.put("acemQcResult", normalizeAcemQcResult(acemQcResult));
                    qcDetail.remove("acemResult");
                    Object certificates = qcDetail.get("qcCertificate");
                    if (certificates instanceof List<?> certList) {
                        qcDetail.put("qcCertificate", normalizeCertificateList(certList));
                    }
                    normalizedDetails.add(qcDetail);
                }
                material.put("qcDetails", normalizedDetails);
            }
            normalizedMaterials.add(material);
        }
        copy.put("materials", normalizedMaterials);
        return copy;
    }


    /**
     * ACEM QC Result follows the specification like RMS analysed result:
     * numeric when the master spec has a min/max, alphanumeric when the range is N/A.
     * Out-of-range values are not rejected.
     */
    private void validateRevalidationAcemQcResults(
            List<QcDivisionPayloads.DivisionDetailInput> divisionDetails) {
        if (divisionDetails == null || divisionDetails.isEmpty()) {
            return;
        }
        List<QcDivisionPayloads.RawMaterialRevalidationData> revalidations = divisionDetails.stream()
                .filter(Objects::nonNull)
                .map(QcDivisionPayloads.DivisionDetailInput::getData)
                .filter(QcDivisionPayloads.RawMaterialRevalidationData.class::isInstance)
                .map(QcDivisionPayloads.RawMaterialRevalidationData.class::cast)
                .filter(data -> data.getMaterials() != null && !data.getMaterials().isEmpty())
                .toList();
        if (revalidations.isEmpty()) {
            return;
        }

        List<String> ingredients = revalidations.stream()
                .flatMap(data -> data.getMaterials().stream())
                .filter(Objects::nonNull)
                .map(QcDivisionPayloads.RevalidationMaterial::getIngredient)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        Map<String, MaterialMaster> mastersByKey = new HashMap<>();
        if (!ingredients.isEmpty()) {
            Stream.concat(
                            materialRepo.findByMaterialCodeIn(ingredients).stream(),
                            materialRepo.findByMaterialNameIn(ingredients).stream())
                    .filter(Objects::nonNull)
                    .forEach(master -> {
                        if (master.getMaterialCode() != null) {
                            mastersByKey.putIfAbsent(master.getMaterialCode().trim().toLowerCase(), master);
                        }
                        if (master.getMaterialName() != null) {
                            mastersByKey.putIfAbsent(master.getMaterialName().trim().toLowerCase(), master);
                        }
                    });
        }

        Map<String, String> errors = new LinkedHashMap<>();
        for (QcDivisionPayloads.RawMaterialRevalidationData data : revalidations) {
            List<QcDivisionPayloads.RevalidationMaterial> materials = data.getMaterials();
            for (int m = 0; m < materials.size(); m++) {
                QcDivisionPayloads.RevalidationMaterial material = materials.get(m);
                if (material == null || material.getQcDetails() == null) {
                    continue;
                }
                String ingredient = material.getIngredient() == null ? "" : material.getIngredient().trim();
                MaterialMaster master = mastersByKey.get(ingredient.toLowerCase());
                List<QcDivisionPayloads.RevalidationQcDetail> qcDetails = material.getQcDetails();
                for (int d = 0; d < qcDetails.size(); d++) {
                    QcDivisionPayloads.RevalidationQcDetail qcDetail = qcDetails.get(d);
                    if (qcDetail == null) {
                        continue;
                    }
                    String value = qcDetail.getAcemQcResult() == null ? null : qcDetail.getAcemQcResult().trim();
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    String fieldPath = "materials[" + m + "].qcDetails[" + d + "]";
                    MaterialSpecification matched = resolveRevalidationMasterSpec(master, qcDetail);
                    if (matched != null) {
                        SpecResultFormatValidator.validateSpecResultFormat(
                                value, matched, fieldPath, "acemQcResult", false, errors);
                    } else if (SpecResultFormatValidator.specificationLabelImpliesNumeric(
                            qcDetail.getSpecification())) {
                        if (!SpecResultFormatValidator.isParseableNumeric(value)) {
                            errors.put(fieldPath + ".acemQcResult", "acemQcResult must be numeric");
                        }
                    } else if (!SpecResultFormatValidator.matchesAlphaNum(value)) {
                        errors.put(fieldPath + ".acemQcResult", "acemQcResult is invalid");
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
        }
    }

    private MaterialSpecification resolveRevalidationMasterSpec(
            MaterialMaster master,
            QcDivisionPayloads.RevalidationQcDetail qcDetail) {
        String parameter = qcDetail.getParameter() == null ? "" : qcDetail.getParameter().trim();
        if (master == null || master.getSpecifications() == null || parameter.isEmpty()) {
            return null;
        }
        for (MaterialSpecification specification : master.getSpecifications()) {
            if (specification == null || specification.getSpecificationName() == null) {
                continue;
            }
            if (specification.getSpecificationName().trim().equalsIgnoreCase(parameter)) {
                return specification;
            }
        }
        return null;
    }

    private String normalizeAcemQcResult(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                return null;
            }
            if (doubleValue == Math.rint(doubleValue)
                    && !Double.isInfinite(doubleValue)
                    && Math.abs(doubleValue) <= Long.MAX_VALUE) {
                return Long.toString((long) doubleValue);
            }
            return Double.toString(doubleValue);
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        if (value instanceof Map<?, ?> map) {
            Object parsedValue = map.get("parsedValue");
            if (parsedValue != null) {
                return normalizeAcemQcResult(parsedValue);
            }
            Object source = map.get("source");
            if (source != null) {
                return normalizeAcemQcResult(source);
            }
        }
        String asText = String.valueOf(value).trim();
        return asText.isEmpty() || "null".equalsIgnoreCase(asText) ? null : asText;
    }

    private Map<String, Object> normalizeHardwareWireData(
            DivisionDetail detail,
            Map<String, Object> data) {
        if (data == null || !"HARDWARE".equals(effectiveDivision(detail))) {
            return data;
        }
        Map<String, Object> copy = new LinkedHashMap<>(data);
        Object motorsNode = copy.get("motorDetails");
        if (!(motorsNode instanceof List<?> motors)) {
            return copy;
        }
        List<Object> normalizedMotors = new ArrayList<>();
        for (Object motorObj : motors) {
            if (!(motorObj instanceof Map<?, ?> motorMap)) {
                normalizedMotors.add(motorObj);
                continue;
            }
            Map<String, Object> motor = new LinkedHashMap<>();
            motorMap.forEach((k, v) -> motor.put(String.valueOf(k), v));
            Object attachmentsNode = motor.get("attachments");
            if (attachmentsNode instanceof Map<?, ?> attachmentsMap) {
                Map<String, Object> attachments = new LinkedHashMap<>();
                attachmentsMap.forEach((k, v) -> attachments.put(String.valueOf(k), v));
                normalizeHardwareAttachmentField(attachments, "uploadReport");
                normalizeHardwareAttachmentField(attachments, "uploadGraph");
                normalizeHardwareAttachmentField(attachments, "uploadPhoto");
                motor.put("attachments", attachments);
            }
            normalizedMotors.add(motor);
        }
        copy.put("motorDetails", normalizedMotors);
        return copy;
    }

    private void normalizeHardwareAttachmentField(Map<String, Object> attachments, String field) {
        Object value = attachments.get(field);
        if (value instanceof List<?> list) {
            attachments.put(field, normalizeCertificateList(list));
        }
    }

    private List<Map<String, Object>> normalizeCertificateList(List<?> certificates) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Object entry : certificates) {
            if (entry == null) {
                continue;
            }
            if (entry instanceof String fileId) {
                String trimmed = fileId.trim();
                if (!trimmed.isEmpty()) {
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("fileId", trimmed);
                    normalized.add(doc);
                }
                continue;
            }
            if (entry instanceof Map<?, ?> map) {
                Map<String, Object> doc = new LinkedHashMap<>();
                map.forEach((k, v) -> doc.put(String.valueOf(k), v));
                Object fileId = doc.get("fileId");
                if (fileId != null && !String.valueOf(fileId).isBlank()) {
                    normalized.add(doc);
                }
            }
        }
        return normalized;
    }

    private void enrichAttachmentList(List<DocumentDto> documents, Long sourceSubDepartmentId, String motorId) {
        if (documents == null) {
            return;
        }
        for (DocumentDto document : documents) {
            enrichDocumentMetadata(document, sourceSubDepartmentId, motorId);
        }
    }

    private void enrichDocumentMetadata(DocumentDto doc, Long sourceSubDepartmentId, String motorId) {
        fileMetadataEnrichmentService.enrichDocument(doc, sourceSubDepartmentId, motorId);
    }

    private Object enrichQualityCheckMetadata(
            Object value,
            Map<String, QualityCheckParams> qualityChecksById) {
        if (value instanceof QualityCheckParameterDto parameter) {
            QualityCheckParams master = qualityChecksById.get(parameter.getParameterId());
            parameter.setParameterName(master == null ? null : master.getParameterName());
            parameter.setSpecification(master == null ? null : master.getSpecification());
            return parameter;
        }
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> copy = new LinkedHashMap<>();
            source.forEach((key, item) -> copy.put(
                    String.valueOf(key),
                    enrichQualityCheckMetadata(item, qualityChecksById)));
            String parameterId = stringValue(copy.get("parameterId"));
            if (parameterId != null) {
                QualityCheckParams master = qualityChecksById.get(parameterId);
                if (master != null) {
                    copy.put("parameterName", master.getParameterName());
                    if (copy.get("parameter") == null || stringValue(copy.get("parameter")).isBlank()) {
                        copy.put("parameter", master.getParameterName());
                    }
                    String savedSpecification = stringValue(copy.get("specification"));
                    if (savedSpecification == null || savedSpecification.isBlank()) {
                        copy.put("specification", specificationText(master.getSpecification()));
                    }
                    // Mixing Cycle Master sample count — restore when client omitted noOfSamples.
                    Object savedSamples = copy.get("noOfSamples");
                    if (savedSamples == null && master.getNoOfSamples() != null) {
                        copy.put("noOfSamples", master.getNoOfSamples());
                    }
                }
            }
            return copy;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> enrichQualityCheckMetadata(item, qualityChecksById))
                    .toList();
        }
        return value;
    }

    private String specificationText(SpecificationRangeDto specification) {
        if (specification == null) return null;
        String min = decimalText(specification.getMinValue());
        String max = decimalText(specification.getMaxValue());
        StringBuilder result = new StringBuilder();
        if (min != null) result.append(min);
        if (min != null && max != null) result.append(" - ");
        if (max != null) result.append(max);
        if (specification.getUnit() != null && !specification.getUnit().isBlank()) {
            if (!result.isEmpty()) result.append(' ');
            result.append(specification.getUnit());
        }
        return result.isEmpty() ? null : result.toString();
    }

    private String decimalText(Double value) {
        return value == null ? null
                : java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void syncProgress(
            QcDivision form,
            Aggregation aggregation,
            PersonInfo actor,
            LocalDateTime now,
            boolean finalSubmit) {
        Batch batch = requireBatch(form.getBatchId());
        List<QcStageDivisionStatusDto> divisionStatuses = mergeDivisionUnitStatuses(
                batch,
                qcDivisionStageStatusResolver.compactStatuses(
                        form, aggregation.divisionStatuses(), batch),
                aggregation);
        batchService.syncQcStageProgress(
                form.getBatchId(), form.getSubDepartmentId(), form.getStatus(),
                batchPremixStatuses(aggregation), batchFinalMixStatuses(aggregation),
                batchMotorStatuses(aggregation),
                divisionStatuses,
                finalSubmit ? actor : null,
                now, false, form.getRemarks(), form.getRejectionReason());
    }

    private void syncApproval(
            QcDivision form,
            Aggregation aggregation,
            String approverUserId,
            LocalDateTime now,
            boolean approvedUnitEvent) {
        Batch batch = requireBatch(form.getBatchId());
        List<QcStageDivisionStatusDto> divisionStatuses = mergeDivisionUnitStatuses(
                batch,
                qcDivisionStageStatusResolver.compactStatuses(
                        form, aggregation.divisionStatuses(), batch),
                aggregation);
        batchService.syncQcStageApproval(
                form.getBatchId(), form.getSubDepartmentId(), form.getStatus(),
                batchPremixStatuses(aggregation), batchFinalMixStatuses(aggregation),
                batchMotorStatuses(aggregation),
                divisionStatuses,
                approvedUnitEvent,
                form.getRemarks(), form.getRejectionReason(), approverUserId, now);
    }

    private void overlayPremixUnitStatus(
            PremixStatusResponse unit, QcPremixStatusDto status) {
        if (status.getPremixSubmissionStatus() == null) {
            return;
        }
        if (status.getPremixSubmissionStatus() == Status.YET_TO_START) {
            if (unit.getPremixSubmissionStatus() == null
                    || unit.getPremixSubmissionStatus() == Status.TO_BE_INITIATED
                    || unit.getPremixSubmissionStatus() == Status.YET_TO_START) {
                unit.setPremixSubmissionStatus(Status.YET_TO_START);
                unit.setPremixSubmissionType(Status.DRAFT);
            }
            return;
        }
        unit.setPremixSubmissionStatus(status.getPremixSubmissionStatus());
        unit.setPremixSubmissionType(status.getPremixSubmissionType());
    }

    /**
     * RMP aggregation uses null stageType; divisionStatuses seed uses PREMIX.
     */
    private static boolean premixStageMatches(StageType unitStage, String aggregationStage) {
        if ("FINAL_MIX".equals(aggregationStage) || unitStage == StageType.FINAL_MIX) {
            return unitStage == StageType.FINAL_MIX && "FINAL_MIX".equals(aggregationStage);
        }
        return aggregationStage == null
                || "PREMIX".equals(aggregationStage)
                || unitStage == null
                || unitStage == StageType.PREMIX;
    }

    private List<QcStageDivisionStatusDto> mergeDivisionUnitStatuses(
            Batch batch,
            List<QcStageDivisionStatusDto> divisionStatuses,
            Aggregation aggregation) {
        if (divisionStatuses == null) {
            return null;
        }
        for (QcStageDivisionStatusDto division : divisionStatuses) {
            String name = QcDivisionCatalog.normalizeDivisionName(
                    division.getDivisionName() != null
                            ? division.getDivisionName()
                            : QcDivisionCatalog.canonicalName(division.getDivisionId()));
            // Do not overlay aggregation onto locked catalog rows — that promoted
            // YET_TO_START → TO_BE_INITIATED and unlocked gated divisions early.
            boolean divisionLocked = division.getStatus() == null
                    || division.getStatus() == Status.YET_TO_START;
            if (!divisionLocked && division.getPremixStatuses() != null) {
                for (PremixStatusResponse unit : division.getPremixStatuses()) {
                    StageType unitStage = unit.getStageType() == null
                            ? StageType.PREMIX : unit.getStageType();
                    aggregation.premixStatuses().stream()
                            .filter(status -> Objects.equals(status.getDivision(), name))
                            .filter(status -> status.getPremixNo() == unit.getPremixNo())
                            .filter(status -> premixStageMatches(unitStage, status.getStageType()))
                            .findFirst()
                            .ifPresent(status -> overlayPremixUnitStatus(unit, status));
                    // Heal divisionStatuses lag: QC stage already APPROVED this premix.
                    if (unit.getPremixSubmissionStatus() != Status.APPROVED
                            && QcDivisionProgressHelper.isPremixUnitApproved(
                            batch, name, unit.getPremixNo(), unitStage)) {
                        unit.setPremixSubmissionStatus(Status.APPROVED);
                        unit.setPremixSubmissionType(Status.SUBMIT);
                    }
                    // Heal premature unlock when manufacturing has not approved this unit.
                    if (("RAW_MATERIAL_PROCESSING".equals(name) || "MIXING".equals(name))
                            && !QcDivisionProgressHelper.isPremixUnitUnlocked(
                            batch, name, unit.getPremixNo(), unitStage)
                            && (unit.getPremixSubmissionStatus() == null
                            || unit.getPremixSubmissionStatus() == Status.TO_BE_INITIATED
                            || unit.getPremixSubmissionStatus() == Status.YET_TO_START)) {
                        unit.setPremixSubmissionStatus(Status.YET_TO_START);
                        unit.setPremixSubmissionType(Status.DRAFT);
                    }
                }
            }
            if (!divisionLocked && division.getFinalMixStatuses() != null) {
                for (PremixStatusResponse unit : division.getFinalMixStatuses()) {
                    aggregation.premixStatuses().stream()
                            .filter(status -> Objects.equals(status.getDivision(), name))
                            .filter(status -> status.getPremixNo() == unit.getPremixNo())
                            .filter(status -> "FINAL_MIX".equals(status.getStageType()))
                            .findFirst()
                            .ifPresent(status -> overlayPremixUnitStatus(unit, status));
                    if (unit.getPremixSubmissionStatus() != Status.APPROVED
                            && QcDivisionProgressHelper.isPremixUnitApproved(
                            batch, name, unit.getPremixNo(), StageType.FINAL_MIX)) {
                        unit.setPremixSubmissionStatus(Status.APPROVED);
                        unit.setPremixSubmissionType(Status.SUBMIT);
                    }
                    if ("MIXING".equals(name)
                            && !QcDivisionProgressHelper.isPremixUnitUnlocked(
                            batch, name, unit.getPremixNo(), StageType.FINAL_MIX)
                            && (unit.getPremixSubmissionStatus() == null
                            || unit.getPremixSubmissionStatus() == Status.TO_BE_INITIATED
                            || unit.getPremixSubmissionStatus() == Status.YET_TO_START)) {
                        unit.setPremixSubmissionStatus(Status.YET_TO_START);
                        unit.setPremixSubmissionType(Status.DRAFT);
                    }
                }
            }
            if (!divisionLocked && division.getMotorStatuses() != null) {
                for (MotorStatusResponse unit : division.getMotorStatuses()) {
                    aggregation.motorStatuses().stream()
                            .filter(status -> Objects.equals(status.getDivision(), name))
                            .filter(status -> Objects.equals(status.getMotorId(), unit.getMotorId()))
                            .findFirst()
                            .ifPresent(status -> {
                                if (status.getMotorSubmissionStatus() != null
                                        && status.getMotorSubmissionStatus() != Status.YET_TO_START) {
                                    unit.setMotorSubmissionStatus(status.getMotorSubmissionStatus());
                                    unit.setMotorSubmissionType(status.getMotorSubmissionType());
                                }
                            });
                    // Heal divisionStatuses lag from QC stage motorStatuses APPROVED.
                    if (unit.getMotorSubmissionStatus() != Status.APPROVED
                            && QcDivisionProgressHelper.isMotorUnitApproved(
                            batch, name, unit.getMotorId())) {
                        unit.setMotorSubmissionStatus(Status.APPROVED);
                        unit.setMotorSubmissionType(Status.SUBMIT);
                    }
                }
            }
            if (division.getStatus() == Status.YET_TO_START
                    && QcDivisionProgressHelper.isDivisionUnlocked(batch, name)) {
                // Preserve manufacturing unlock already on the batch, except heal gated families.
                boolean ndtOk = !isNdtFamilyDivision(name) || hasManufacturingNdtApprovalUnlock(batch);
                boolean stfOk = !isStfFamilyDivision(name)
                        || MainBatchStageProgressHelper.hasApprovedBemMotor(batch);
                if (ndtOk && stfOk) {
                    division.setStatus(Status.TO_BE_INITIATED);
                }
            }
        }
        reconcileNdtLocksAgainstManufacturing(batch, divisionStatuses);
        reconcileStfFamilyLocksAgainstBemApproval(batch, divisionStatuses);
        reconcileStfMotorLocksAgainstManufacturing(batch, divisionStatuses);
        return divisionStatuses;
    }

    private static boolean isNdtFamilyDivision(String divisionName) {
        return QcDivisionCatalog.NDT.equals(QcDivisionCatalog.normalizeDivisionName(divisionName));
    }

    private static boolean isStfFamilyDivision(String divisionName) {
        String name = QcDivisionCatalog.normalizeDivisionName(divisionName);
        return QcDivisionCatalog.STATIC_TEST_FACILITY.equals(name)
                || QcDivisionCatalog.QC.equals(name)
                || QcDivisionCatalog.WEIGHTMENT.equals(name);
    }

    private Set<String> manufacturingNdtApprovedMotorIds(Batch batch) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.NDT)
                .map(stage -> Optional.ofNullable(stage.getMotorStatuses()).orElseGet(List::of).stream()
                        .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED)
                        .map(MotorStatusResponse::getMotorId)
                        .filter(Objects::nonNull)
                        .filter(id -> !id.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(LinkedHashSet::new);
    }

    private boolean isManufacturingNdtStageApproved(Batch batch) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.NDT)
                .map(stage -> stage.getStatus() == Status.COMPLETELY_APPROVED
                        || stage.getStatus() == Status.APPROVED)
                .orElse(false);
    }

    private boolean hasManufacturingNdtApprovalUnlock(Batch batch) {
        return isManufacturingNdtStageApproved(batch)
                || !manufacturingNdtApprovedMotorIds(batch).isEmpty();
    }

    /**
     * Heal corrupted NDT unlocks written while manufacturing NDT was only TO_BE_INITIATED.
     * Manufacturing NDT APPROVED is the unlock trigger for QC NDT only.
     */
    private void reconcileNdtLocksAgainstManufacturing(
            Batch batch,
            List<QcStageDivisionStatusDto> divisionStatuses) {
        if (batch == null || divisionStatuses == null) {
            return;
        }
        if (QcDivisionProgressHelper.isQualificationSubscale(batch)
                || QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            return;
        }
        if (isManufacturingNdtStageApproved(batch)) {
            return;
        }
        Set<String> approvedMotors = manufacturingNdtApprovedMotorIds(batch);
        for (QcStageDivisionStatusDto division : divisionStatuses) {
            String name = QcDivisionCatalog.normalizeDivisionName(
                    division.getDivisionName() != null
                            ? division.getDivisionName()
                            : QcDivisionCatalog.canonicalName(division.getDivisionId()));
            if (!isNdtFamilyDivision(name)) {
                continue;
            }
            reconcileMotorUnitsAgainstApprovedSet(division, approvedMotors);
        }
    }

    /**
     * Heal STF / QC / WEIGHTMENT unlocks until any STF BEM motor is APPROVED.
     */
    private void reconcileStfFamilyLocksAgainstBemApproval(
            Batch batch,
            List<QcStageDivisionStatusDto> divisionStatuses) {
        if (batch == null || divisionStatuses == null) {
            return;
        }
        if (QcDivisionProgressHelper.isQualificationSubscale(batch)
                || QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            return;
        }
        if (MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
            return;
        }
        for (QcStageDivisionStatusDto division : divisionStatuses) {
            String name = QcDivisionCatalog.normalizeDivisionName(
                    division.getDivisionName() != null
                            ? division.getDivisionName()
                            : QcDivisionCatalog.canonicalName(division.getDivisionId()));
            if (!isStfFamilyDivision(name)) {
                continue;
            }
            reconcileMotorUnitsAgainstApprovedSet(division, Set.of());
        }
    }

    /**
     * After STF BEM unlocks the division family: keep QC STATIC_TEST_FACILITY motor units
     * locked unless that motor is APPROVED on manufacturing STF (MAIN and BEM).
     */
    private void reconcileStfMotorLocksAgainstManufacturing(
            Batch batch,
            List<QcStageDivisionStatusDto> divisionStatuses) {
        if (batch == null || divisionStatuses == null) {
            return;
        }
        if (QcDivisionProgressHelper.isQualificationSubscale(batch)
                || QcDivisionProgressHelper.isExperimentalSubscale(batch)) {
            return;
        }
        if (!MainBatchStageProgressHelper.hasApprovedBemMotor(batch)) {
            return;
        }
        // Backfill missing division-12 row on read so unlock/status can apply.
        boolean hasStfDivision = divisionStatuses.stream().anyMatch(division -> {
            String name = QcDivisionCatalog.normalizeDivisionName(
                    division.getDivisionName() != null
                            ? division.getDivisionName()
                            : QcDivisionCatalog.canonicalName(division.getDivisionId()));
            return QcDivisionCatalog.STATIC_TEST_FACILITY.equals(name)
                    || Objects.equals(division.getDivisionId(), QcDivisionCatalog.STATIC_TEST_FACILITY_ID);
        });
        if (!hasStfDivision) {
            QcDivisionProgressHelper.ensureDivision(
                    batch,
                    QcDivisionCatalog.STATIC_TEST_FACILITY_ID,
                    QcDivisionCatalog.STATIC_TEST_FACILITY);
            QcDivisionProgressHelper.unlockDivision(batch, QcDivisionCatalog.STATIC_TEST_FACILITY);
            QcDivisionProgressHelper.findDivision(batch, QcDivisionCatalog.STATIC_TEST_FACILITY)
                    .ifPresent(divisionStatuses::add);
        }

        Set<String> approvedMotors = manufacturingStfApprovedMotorIds(batch);
        for (QcStageDivisionStatusDto division : divisionStatuses) {
            String name = QcDivisionCatalog.normalizeDivisionName(
                    division.getDivisionName() != null
                            ? division.getDivisionName()
                            : QcDivisionCatalog.canonicalName(division.getDivisionId()));
            if (!QcDivisionCatalog.STATIC_TEST_FACILITY.equals(name)
                    && !Objects.equals(division.getDivisionId(), QcDivisionCatalog.STATIC_TEST_FACILITY_ID)) {
                continue;
            }
            // Seed all mfg STF motors onto the QC division row (locked unless approved).
            MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.STF)
                    .map(Batch.StageProgress::getMotorStatuses)
                    .orElseGet(List::of)
                    .forEach(status -> {
                        if (status == null || status.getMotorId() == null || status.getMotorId().isBlank()) {
                            return;
                        }
                        boolean approved = approvedMotors.contains(status.getMotorId());
                        QcDivisionProgressHelper.ensureMotorUnit(
                                batch,
                                QcDivisionCatalog.STATIC_TEST_FACILITY,
                                status.getMotorId(),
                                status.getSubType(),
                                approved);
                    });
            QcDivisionProgressHelper.findDivision(batch, QcDivisionCatalog.STATIC_TEST_FACILITY)
                    .ifPresent(updated -> {
                        division.setMotorStatuses(updated.getMotorStatuses());
                        if (updated.getStatus() != null) {
                            division.setStatus(updated.getStatus());
                        }
                    });
            reconcileMotorUnitsAgainstApprovedSet(division, approvedMotors);
            // Division stays open when any BEM is approved even if some motors remain locked.
            if (division.getStatus() == null || division.getStatus() == Status.YET_TO_START) {
                division.setStatus(Status.TO_BE_INITIATED);
            }
        }
    }

    private Set<String> manufacturingStfApprovedMotorIds(Batch batch) {
        return MainBatchStageProgressHelper.findStage(batch, MainBatchStageConstants.STF)
                .map(stage -> Optional.ofNullable(stage.getMotorStatuses()).orElseGet(List::of).stream()
                        .filter(status -> status.getMotorSubmissionStatus() == Status.APPROVED)
                        .map(MotorStatusResponse::getMotorId)
                        .filter(Objects::nonNull)
                        .filter(id -> !id.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .orElseGet(LinkedHashSet::new);
    }

    private void reconcileMotorUnitsAgainstApprovedSet(
            QcStageDivisionStatusDto division,
            Set<String> approvedMotors) {
        if (division.getMotorStatuses() != null) {
            for (MotorStatusResponse unit : division.getMotorStatuses()) {
                String motorId = unit.getMotorId();
                Status status = unit.getMotorSubmissionStatus();
                if (motorId != null && approvedMotors.contains(motorId)) {
                    if (status == null || status == Status.YET_TO_START) {
                        unit.setMotorSubmissionType(Status.DRAFT);
                        unit.setMotorSubmissionStatus(Status.TO_BE_INITIATED);
                    }
                    continue;
                }
                if (status == null
                        || status == Status.YET_TO_START
                        || status == Status.TO_BE_INITIATED
                        || status == Status.DRAFT) {
                    unit.setMotorSubmissionType(Status.DRAFT);
                    unit.setMotorSubmissionStatus(Status.YET_TO_START);
                }
            }
        }
        boolean anyUnlocked = division.getMotorStatuses() != null
                && division.getMotorStatuses().stream()
                .anyMatch(unit -> unit.getMotorSubmissionStatus() != null
                        && unit.getMotorSubmissionStatus() != Status.YET_TO_START);
        if (!anyUnlocked && approvedMotors.isEmpty()) {
            division.setStatus(Status.YET_TO_START);
        } else if (division.getStatus() == null || division.getStatus() == Status.YET_TO_START) {
            if (anyUnlocked || !approvedMotors.isEmpty()) {
                division.setStatus(Status.TO_BE_INITIATED);
            }
        }
    }

    private boolean hasStfEligibleMotor(
            QcDivision form,
            Batch batch,
            Aggregation aggregation) {
        boolean rawMaterialRevalidationApproved = aggregation.divisionStatuses().stream()
                .anyMatch(status -> "RAW_MATERIAL_REVALIDATION".equals(status.getDivision())
                        && status.getStatus() == Status.APPROVED);
        if (!rawMaterialRevalidationApproved) {
            return false;
        }

        List<String> mainMotorIds = Optional.ofNullable(batch.getMotorIds())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motorId -> !motorId.isBlank())
                .toList();

        // BEM-only subscale: no main motors — unlock STF after RMR if BEM moulds exist.
        if (mainMotorIds.isEmpty()) {
            SubScaleMetadata metadata =
                    subscaleMetadataResolver.resolveApprovedMetadata(batch.getBatchId());
            return !subscaleMetadataResolver.toBemMotorIds(metadata).isEmpty();
        }

        Set<String> requiredMotorDivisions = workflowDivisions(form, MOTOR_DIVISIONS);
        if (requiredMotorDivisions.isEmpty()) {
            return false;
        }

        List<String> motorCandidates = mainMotorIds.stream()
                .filter(motorId -> motorApprovedInAllQcDivisions(
                        motorId, requiredMotorDivisions, aggregation))
                .toList();
        if (motorCandidates.isEmpty()) {
            return false;
        }

        StfPremixLinkContext premixLinks = loadStfPremixLinkContext(batch.getBatchId());
        return motorCandidates.stream().anyMatch(motorId -> linkedPremixesApproved(
                form, batch.getBatchId(), motorId, premixLinks, aggregation));
    }

    private Set<String> workflowDivisions(
            QcDivision form,
            Set<String> supportedDivisions) {
        return Optional.ofNullable(form.getDivisionWorkflow())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(QcDivisionWorkflow::getDivision)
                .filter(supportedDivisions::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean motorApprovedInAllQcDivisions(
            String motorId,
            Set<String> requiredDivisions,
            Aggregation aggregation) {
        return requiredDivisions.stream().allMatch(division -> {
            List<QcMotorStatusDto> statuses = aggregation.motorStatuses().stream()
                    .filter(status -> division.equals(status.getDivision()))
                    .filter(status -> motorId.equals(status.getMotorId()))
                    .toList();
            return !statuses.isEmpty() && statuses.stream().allMatch(status ->
                    status.getMotorSubmissionStatus() == Status.APPROVED);
        });
    }

    private StfPremixLinkContext loadStfPremixLinkContext(String batchId) {
        Map<String, Set<String>> bowlIdsByMotor = castingCuringRepo
                .findByBatchIdAndIsDeletedFalse(batchId)
                .map(CastingCuring::getMotors)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(motor -> motor.getMotorId() != null && !motor.getMotorId().isBlank())
                .collect(Collectors.toMap(
                        CastingCuringMotor::getMotorId,
                        this::extractCastingBowlIds,
                        (first, second) -> {
                            Set<String> merged = new LinkedHashSet<>(first);
                            merged.addAll(second);
                            return merged;
                        },
                        LinkedHashMap::new));

        Map<String, Set<Integer>> premixNumbersByBowlId = new LinkedHashMap<>();
        mixingRepo.findByBatchIdAndIsDeletedFalse(batchId)
                .map(Mixing::getMixingDetails)
                .map(MixingDetailsDto::getStages)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .filter(stage -> stage.getStageType() == StageType.FINAL_MIX)
                .map(MixingStageDto::getPremixes)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(premix -> premix.getPremixNo() != null)
                .forEach(premix -> {
                    String bowlId = premix.getMixerConfiguration() == null
                            ? null : normalizeBowlId(premix.getMixerConfiguration().getBowlId());
                    if (bowlId != null) {
                        premixNumbersByBowlId
                                .computeIfAbsent(bowlId, ignored -> new LinkedHashSet<>())
                                .add(premix.getPremixNo());
                    }
                });
        return new StfPremixLinkContext(bowlIdsByMotor, premixNumbersByBowlId);
    }

    private Set<String> extractCastingBowlIds(CastingCuringMotor motor) {
        if (motor == null
                || motor.getCastingSections() == null
                || motor.getCastingSections().getCastingProcess() == null) {
            return Collections.emptySet();
        }
        CastingProcess process = motor.getCastingSections().getCastingProcess();
        Set<String> bowlIds = new LinkedHashSet<>();
        Optional.ofNullable(process.getFinalMixBowlDetails())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(FinalMixBowlDetail::getBowlId)
                .map(this::normalizeBowlId)
                .filter(Objects::nonNull)
                .forEach(bowlIds::add);
        Optional.ofNullable(process.getCastingFromBowlDetails())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .map(CastingFromBowlDetail::getBowlId)
                .map(this::normalizeBowlId)
                .filter(Objects::nonNull)
                .forEach(bowlIds::add);
        return bowlIds;
    }

    private String normalizeBowlId(String bowlId) {
        if (bowlId == null || bowlId.isBlank()) {
            return null;
        }
        return bowlId.trim().toUpperCase(Locale.ROOT);
    }

    private boolean linkedPremixesApproved(
            QcDivision form,
            String batchId,
            String motorId,
            StfPremixLinkContext links,
            Aggregation aggregation) {
        Set<String> bowlIds = links.bowlIdsByMotor().getOrDefault(
                motorId, Collections.emptySet());
        if (bowlIds.isEmpty()) {
            log.warn("No Casting/Curing bowl link found for batch {} motor {}; "
                    + "using motor-only QC eligibility", batchId, motorId);
            return true;
        }

        Set<Integer> premixNumbers = new LinkedHashSet<>();
        for (String bowlId : bowlIds) {
            Set<Integer> matches = links.premixNumbersByBowlId().get(bowlId);
            if (matches == null || matches.size() != 1) {
                log.warn("Casting/Curing bowl {} for batch {} motor {} does not map "
                                + "uniquely to a Mixing Final Mix; using motor-only QC eligibility",
                        bowlId, batchId, motorId);
                return true;
            }
            premixNumbers.add(matches.iterator().next());
        }

        Set<String> requiredPremixDivisions = workflowDivisions(form, PREMIX_DIVISIONS);
        return premixNumbers.stream().allMatch(premixNo ->
                premixApprovedInAllQcDivisions(
                        premixNo, requiredPremixDivisions, aggregation));
    }

    private boolean premixApprovedInAllQcDivisions(
            int premixNo,
            Set<String> requiredDivisions,
            Aggregation aggregation) {
        return requiredDivisions.stream().allMatch(division -> {
            List<QcPremixStatusDto> statuses = aggregation.premixStatuses().stream()
                    .filter(status -> division.equals(status.getDivision()))
                    .filter(status -> status.getPremixNo() == premixNo)
                    .toList();
            if (statuses.isEmpty() || statuses.stream().anyMatch(status ->
                    status.getPremixSubmissionStatus() != Status.APPROVED)) {
                return false;
            }
            if (!"MIXING".equals(division)) {
                return true;
            }
            return statuses.stream().anyMatch(status ->
                    "PREMIX".equals(status.getStageType()))
                    && statuses.stream().anyMatch(status ->
                    "FINAL_MIX".equals(status.getStageType()));
        });
    }

    private List<MotorStatusResponse> batchMotorStatuses(Aggregation aggregation) {
        return aggregation.motorStatuses().stream()
                .map(status -> MotorStatusResponse.builder()
                        .division(status.getDivision())
                        .subType(status.getSubType())
                        .motorId(status.getMotorId())
                        .motorSubmissionType(status.getMotorSubmissionType())
                        .motorSubmissionStatus(status.getMotorSubmissionStatus())
                        .build())
                .toList();
    }

    private List<QcPremixStatusDto> qcPremixStatuses(Aggregation aggregation) {
        return aggregation.premixStatuses().stream()
                .filter(status -> !"FINAL_MIX".equals(status.getStageType()))
                .toList();
    }

    private List<QcPremixStatusDto> qcFinalMixStatuses(Aggregation aggregation) {
        return aggregation.premixStatuses().stream()
                .filter(status -> "FINAL_MIX".equals(status.getStageType()))
                .toList();
    }

    private List<PremixStatusResponse> batchPremixStatuses(Aggregation aggregation) {
        return qcPremixStatuses(aggregation).stream()
                .map(this::toPremixStatusResponse)
                .toList();
    }

    private List<PremixStatusResponse> batchFinalMixStatuses(Aggregation aggregation) {
        return qcFinalMixStatuses(aggregation).stream()
                .map(this::toPremixStatusResponse)
                .toList();
    }

    private PremixStatusResponse toPremixStatusResponse(QcPremixStatusDto status) {
        return PremixStatusResponse.builder()
                .division(status.getDivision())
                .subType(status.getSubType())
                .premixNo(status.getPremixNo())
                .stageType(parseStageType(status.getStageType()))
                .premixSubmissionType(status.getPremixSubmissionType())
                .premixSubmissionStatus(status.getPremixSubmissionStatus())
                .build();
    }

    private StageType parseStageType(String value) {
        if (value == null) return null;
        try {
            return StageType.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Map<String, Object> mapValue(Object value, String message) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, message);
        }
        return (Map<String, Object>) map;
    }

    private List<Map<String, Object>> mapList(Object value, String message) {
        if (!(value instanceof Collection<?> collection)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, message);
        }
        List<Map<String, Object>> values = new ArrayList<>();
        for (Object item : collection) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new AcemException(ErrorCode.INVALID_REQUEST, message);
            }
            values.add((Map<String, Object>) map);
        }
        return values;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeDivision(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "division is required");
        }
        return normalized;
    }

    private String canonicalDivision(String value) {
        String division = normalizeDivision(value);
        if ("POST_CURE_OPERATION".equals(division)) {
            division = "POST_CURE";
        }
        // Legacy alias after rename PROPELLANT_PROPERTIES → QC.
        return QcDivisionCatalog.normalizeDivisionName(division);
    }

    private String resolvePayloadDivision(String division, String subType) {
        if (!"RAW_MATERIAL".equals(division)) return canonicalDivision(division);
        if ("RAW_MATERIAL_REVALIDATION".equals(subType)
                || "RAW_MATERIAL_PROCESSING".equals(subType)) {
            return subType;
        }
        throw new AcemException(ErrorCode.INVALID_REQUEST,
                "subType must be RAW_MATERIAL_REVALIDATION or "
                        + "RAW_MATERIAL_PROCESSING when division is RAW_MATERIAL");
    }

    private String effectiveDivision(DivisionDetail detail) {
        String storedDivision = normalizeDivision(detail.getDivision());
        String storedSubType = normalizeNullable(detail.getSubType());
        return "RAW_MATERIAL".equals(storedDivision)
                ? resolvePayloadDivision(storedDivision, storedSubType)
                : canonicalDivision(storedDivision);
    }

    private boolean detailSubTypeMatches(
            DivisionDetail detail, String canonicalDivision, String requestedSubType) {
        if (requestedSubType == null) return true;
        String storedSubType = normalizeNullable(detail.getSubType());
        return Objects.equals(storedSubType, requestedSubType)
                || (storedSubType == null && Objects.equals(canonicalDivision, requestedSubType));
    }

    private String validateQcDivisionIdentity(
            QcDivision form,
            Long divisionId,
            String divisionValue,
            String subTypeValue,
            boolean divisionIdRequired) {
        String requestedDivision = normalizeDivision(divisionValue);
        String division = resolvePayloadDivision(
                requestedDivision, normalizeNullable(subTypeValue));
        if (divisionId == null) {
            if (divisionIdRequired) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "divisionId is required for QC division approval");
            }
            return division;
        }

        String canonicalDivision = QcDivisionCatalog.canonicalName(divisionId);
        if (canonicalDivision == null) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Unsupported QC divisionId: " + divisionId);
        }
        if (!Objects.equals(canonicalDivision, division)) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "divisionId " + divisionId + " does not match division " + division);
        }

        boolean belongsToForm = Optional.ofNullable(form.getDivisionWorkflow())
                .orElseGet(Collections::emptyList)
                .stream()
                .anyMatch(workflow -> Objects.equals(workflow.getDivisionId(), divisionId)
                        && Objects.equals(
                        QcDivisionCatalog.normalizeDivisionName(workflow.getDivision()),
                        canonicalDivision));
        if (!belongsToForm) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "QC division does not belong to the supplied form");
        }
        return canonicalDivision;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private PersonInfo personValue(Object value) {
        if (value instanceof PersonInfo person) return person;
        if (value instanceof Map<?, ?> map) {
            return PersonInfo.builder()
                    .id(stringValue(map.get("id")))
                    .fullName(stringValue(map.get("fullName")))
                    .build();
        }
        return null;
    }

    private LocalDateTime dateTimeValue(Object value) {
        return DateTimeConversionUtil.toLocalDateTime(value);
    }

    private record ActionContext(
            Long divisionId,
            String division,
            String subType,
            String motorId,
            Integer premixNo,
            String stageType,
            Status motorSubmissionType,
            Status motorSubmissionStatus,
            Status premixSubmissionType,
            Status premixSubmissionStatus,
            Status divisionSubmissionType,
            Status divisionStatus,
            String unlockedDivision) {
    }

    private record UnitShape(String unitsKey, String identityKey) {
    }

    private record MixingApprovalTarget(
            DivisionDetail detail,
            Map<String, Object> stageDetails) {
    }

    private record StfPremixLinkContext(
            Map<String, Set<String>> bowlIdsByMotor,
            Map<String, Set<Integer>> premixNumbersByBowlId) {
    }

    private record HardwareProcessValues(String ovenNo, String rh) {
    }

    private record Aggregation(
            List<DivisionStatusDto> divisionStatuses,
            List<QcMotorStatusDto> motorStatuses,
            List<QcPremixStatusDto> premixStatuses,
            boolean allDivisionsApproved,
            int approvedDivisionCount,
            int totalDivisionCount,
            boolean allMotorsApproved,
            int pendingMotorCount,
            int approvedMotorCount,
            int rejectedMotorCount,
            int inProgressMotorCount,
            int totalMotorCount,
            boolean allPremixesApproved,
            int pendingPremixCount,
            int approvedPremixCount,
            int rejectedPremixCount,
            int totalPremixCount) {
    }

    public ApproverStatusResponse changeStatus(ApproverStatusRequest request, String approverName) {
        QcDivision entity = requireForm(request.getFormId());
        validateSubDepartment(entity, request.getSubDepartmentId());
        User approver = userRepo.findByUserId(approverName)
                .orElseThrow(() -> new AcemException(ErrorCode.USER_NOT_FOUND, "User not found"));
        PersonInfo approverInfo = PersonInfo.builder()
                .id(approver.getUserId())
                .fullName(approver.getUsername())
                .build();

        ApprovalStatusUtil.applyStatusChange(request.getActionType(), request.getSubDepartmentId(), request.getRemarks(), request.getRejectionReason(),
                approver.getUsername(), entity::setStatus,
                entity::setRemarks, entity::setRejectionReason, entity::setUpdatedBy, entity::setUpdatedAt
        );
        entity.setUpdatedBy(approverInfo);
        if (request.getActionType() == Status.APPROVED) {
            entity.setApprovedBy(approverInfo);
            entity.setApprovedOn(entity.getUpdatedAt());
        }

        qcDivisionRepo.save(entity);
        Batch batch = requireBatch(entity.getBatchId());
        Aggregation aggregation = aggregate(entity, batch);
        LocalDateTime now = entity.getUpdatedAt() != null
                ? entity.getUpdatedAt() : LocalDateTime.now();
        if (request.getActionType() == Status.APPROVED
                || request.getActionType() == Status.COMPLETELY_APPROVED) {
            syncApproval(entity, aggregation, approverName, now, true);
        } else {
            syncProgress(entity, aggregation, approverInfo, now, false);
        }
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

    public Map<String, Object> buildDivisionBlockchainPayload(String formId, String divisionName, String subType, Object identifier, String approverName, LocalDateTime approvalTime) {
        QcDivision form = qcDivisionRepo.findByFormIdAndIsDeletedFalse(formId)
                .orElseThrow(() -> new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "QC Division form not found: " + formId));

        String canonicalDivision = canonicalDivision(divisionName);
        String normalizedSubType = normalizeNullable(subType);

        DivisionDetail targetDetail = null;
        if (form.getDivisionDetails() != null) {
            for (DivisionDetail detail : form.getDivisionDetails()) {
                if (detail == null) continue;
                String detailEffectiveDivision = effectiveDivision(detail);
                if (Objects.equals(detailEffectiveDivision, canonicalDivision)) {
                    String detailSubType = normalizeNullable(detail.getSubType());
                    if (normalizedSubType == null || Objects.equals(detailSubType, normalizedSubType)) {
                        targetDetail = detail;
                        break;
                    }
                }
            }
        }

        if (targetDetail == null) {
            throw new AcemException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Division details not found for division=" + divisionName + ", subType=" + subType);
        }

        Map<String, Object> rootPayload = new LinkedHashMap<>();

        // 1. Audit / Metadata Object
        Map<String, Object> auditMap = new LinkedHashMap<>();
        if (form.getCreatedBy() != null) {
            auditMap.put("createdBy", form.getCreatedBy().getFullName());
        }
        if (form.getCreatedAt() != null) {
            auditMap.put("createdAt", form.getCreatedAt());
        }
        if (approverName != null) {
            auditMap.put("approvedBy", approverName);
            auditMap.put("approvedAt", approvalTime);
        }
        rootPayload.put("audit", auditMap);

        // 2. Division Details Object
        Map<String, Object> divisionMap = new LinkedHashMap<>();
        divisionMap.put("division", targetDetail.getDivision());
        if (targetDetail.getSubType() != null && !targetDetail.getSubType().isBlank()) {
            divisionMap.put("subType", targetDetail.getSubType());
        }

        Object rawData = targetDetail.getData();
        if (rawData != null) {
            Map<String, Object> extractedData = filterDataForIdentifier(rawData, identifier);
            Map<String, Object> sanitizedData = (Map<String, Object>) transformFilesToHashes(extractedData);
            divisionMap.putAll(sanitizedData);
        }
        rootPayload.put("divisionDetails", divisionMap);

        return rootPayload;
    }

    private Map<String, Object> filterDataForIdentifier(Object rawData, Object identifier) {
        Map<String, Object> sourceMap;
        if (rawData instanceof Map) {
            sourceMap = new LinkedHashMap<>((Map<String, Object>) rawData);
        } else {
            sourceMap = objectMapper.convertValue(rawData, Map.class);
        }

        if (identifier == null) {
            return sourceMap;
        }

        String identifierStr = String.valueOf(identifier).trim();
        String cleanIdentifier = identifierStr.replaceAll("^.*[^0-9]+", "").trim();
        if (cleanIdentifier.isEmpty()) {
            cleanIdentifier = identifierStr; // fallback if no digits found
        }

        if (sourceMap.containsKey("premixes") && sourceMap.get("premixes") instanceof List<?> list) {
            List<Map<String, Object>> filteredPremixes = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> itemMap) {
                    Object pNo = itemMap.get("premixNo");
                    if (pNo != null) {
                        String pNoStr = String.valueOf(pNo).trim();
                        if (pNoStr.equals(identifierStr) || pNoStr.equals(cleanIdentifier)) {
                            filteredPremixes.add((Map<String, Object>) itemMap);
                        }
                    }
                }
            }
            sourceMap.put("premixes", filteredPremixes);
        } else {
            String[] motorKeys = {
                    "motorDetails", "motors", "motorWeightDetails",
                    "postCureMotorDetails", "trimmingDetails", "curingDetails"
            };
            for (String key : motorKeys) {
                if (sourceMap.containsKey(key) && sourceMap.get(key) instanceof List<?> list) {
                    List<Map<String, Object>> filteredList = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> itemMap) {
                            Object mId = itemMap.get("motorId");
                            if (mId == null) {
                                mId = itemMap.get("motorIdNo");
                            }
                            if (mId != null) {
                                String mIdStr = String.valueOf(mId).trim();
                                if (mIdStr.equals(identifierStr) || mIdStr.equals(cleanIdentifier)) {
                                    filteredList.add((Map<String, Object>) itemMap);
                                }
                            }
                        }
                    }
                    sourceMap.put(key, filteredList);
                }
            }
        }

        return sourceMap;
    }

    @SuppressWarnings("unchecked")
    private Object transformFilesToHashes(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof DocumentDto doc) {
            return doc.getFileId() != null ? getFileHash(doc.getFileId()) : null;
        }

        if (data instanceof Map<?, ?> map) {
            if (map.containsKey("fileId") && map.get("fileId") != null) {
                String fileId = String.valueOf(map.get("fileId"));
                return getFileHash(fileId); // Returns the fileHash String
            }

            Map<String, Object> transformedMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                transformedMap.put(key, transformFilesToHashes(entry.getValue()));
            }
            return transformedMap;
        }
        if (data instanceof List<?> list) {
            List<Object> transformedList = new ArrayList<>();
            for (Object item : list) {
                transformedList.add(transformFilesToHashes(item));
            }
            return transformedList;
        }
        if (isPrimitiveOrWrapperOrString(data.getClass())) {
            return data;
        }

        try {
            Map<String, Object> beanMap = objectMapper.convertValue(data, Map.class);
            return transformFilesToHashes(beanMap);
        } catch (Exception e) {
            return data; // Fallback if conversion fails
        }
    }

    private boolean isPrimitiveOrWrapperOrString(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(String.class) ||
                Number.class.isAssignableFrom(clazz) ||
                Boolean.class.isAssignableFrom(clazz) ||
                java.time.temporal.Temporal.class.isAssignableFrom(clazz);
    }
    private String getFileHash(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        return fileBindingService.getFileHash(fileId);
    }
}
