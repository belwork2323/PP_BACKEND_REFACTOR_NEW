package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.materials.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import in.co.bel.acempp.dto.user.operations.MaterialGrade;
import in.co.bel.acempp.entity.MaterialMaster;
import in.co.bel.acempp.entity.MaterialSpecification;
import in.co.bel.acempp.entity.batch.UnitMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.enums.MaterialType;
import in.co.bel.acempp.enums.PreparationType;
import in.co.bel.acempp.enums.RawMaterialCategory;
import in.co.bel.acempp.enums.RmpFormTemplate;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.MaterialRepository;
import in.co.bel.acempp.repository.UnitMasterRepository;
import in.co.bel.acempp.dto.masterdata.MasterDataStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class MaterialMasterDataService {

    private final MaterialRepository repository;
    private final UnitMasterRepository unitMasterRepository;

    public static boolean isActive(MaterialMaster material) {
        return material.getIsActive() == null || Boolean.TRUE.equals(material.getIsActive());
    }

    public static boolean isSpecificationActive(MaterialSpecification specification) {
        return specification == null
                || specification.getIsActive() == null
                || Boolean.TRUE.equals(specification.getIsActive());
    }

    public MaterialMasterListResponse list(MaterialMasterListRequest request) {
        MaterialMasterListRequest req = request == null ? new MaterialMasterListRequest() : request;
        List<MaterialMaster> all = repository.findAll().stream()
                .sorted(MasterDataListOrdering.byLongIdDesc(MaterialMaster::getMaterialId))
                .toList();

        long active = all.stream().filter(MaterialMasterDataService::isActive).count();
        long inactive = all.size() - active;

        List<MaterialMasterRecordDto> items = all.stream()
                .filter(m -> matchesActive(m, req.getIsActive()))
                .filter(m -> matchesType(m, req.getMaterialType()))
                .filter(m -> matchesRawMaterialCategory(m, req.getRawMaterialType()))
                .filter(m -> matchesPreparationType(m, req.getPreparationType()))
                .filter(m -> matchesSearch(m, req.getSearch()))
                .map(this::toDto)
                .toList();

        return MaterialMasterListResponse.builder()
                .items(items)
                .stats(MasterDataStatsDto.builder()
                        .total(all.size())
                        .active(active)
                        .inactive(inactive)
                        .build())
                .build();
    }

    public MaterialMasterRecordDto create(MaterialMasterCreateRequest request) {
        String code = MasterDataValidation.validateCode(request.getMaterialCode());
        if (repository.findFirstByMaterialCode(code).isPresent()) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Material code already exists: " + code);
        }
        AtomicLong specCodeCounter = new AtomicLong(1L);
        List<MaterialGrade> grades = mapGrades(request.getGrades(), null, specCodeCounter);
        List<MaterialSpecification> specs = mapSpecsForCreate(request.getSpecifications(), specCodeCounter);
        validateTree(grades, specs);
        RawMaterialCategory rawMaterialType = resolveRawMaterialCategory(request.getRawMaterialType());
        PreparationType preparationType = validateRawMaterialClassification(
                rawMaterialType, request.getPreparationType());
        RmpFormTemplate rmpFormTemplate = validateRmpFormTemplate(
                request.getRmpFormTemplate(), request.getMaterialType(), grades);

        MaterialMaster entity = MaterialMaster.builder()
                .materialId(nextId())
                .materialCode(code)
                .materialName(MasterDataValidation.validateName(request.getMaterialName()))
                .materialType(request.getMaterialType())
                .rawMaterialType(rawMaterialType)
                .preparationType(preparationType)
                .rmpFormTemplate(rmpFormTemplate)
                .grades(grades)
                .specifications(specs)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    public MaterialMasterRecordDto update(MaterialMasterUpdateRequest request) {
        MaterialMaster entity = repository.findFirstByMaterialId(request.getMaterialId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Material not found"));
        if (request.getMaterialCode() != null && !request.getMaterialCode().isBlank()
                && !Objects.equals(entity.getMaterialCode(), request.getMaterialCode().trim())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Material code cannot be changed");
        }
        RawMaterialCategory existingCategory = resolveRawMaterialCategory(entity.getRawMaterialType());
        if (request.getRawMaterialType() != null
                && request.getRawMaterialType() != existingCategory) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Raw material type cannot be changed");
        }
        if (!Objects.equals(request.getPreparationType(), entity.getPreparationType())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Preparation type cannot be changed");
        }
        RmpFormTemplate existingTemplate = resolveRmpFormTemplate(entity);
        if (request.getRmpFormTemplate() != null
                && request.getRmpFormTemplate() != existingTemplate) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "RMP form template cannot be changed");
        }
        AtomicLong specCodeCounter = new AtomicLong(1L);
        List<MaterialGrade> grades = mapGrades(request.getGrades(), entity.getGrades(), specCodeCounter);
        List<MaterialSpecification> specs = mapSpecsForUpdate(
                request.getSpecifications(), entity.getSpecifications(), specCodeCounter);
        validateTree(grades, specs);
        // Re-validate existing template against updated material type / grades
        validateRmpFormTemplate(existingTemplate, request.getMaterialType(), grades);

        entity.setMaterialName(MasterDataValidation.validateName(request.getMaterialName()));
        entity.setMaterialType(request.getMaterialType());
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        entity.setGrades(grades);
        entity.setSpecifications(specs);
        // Backfill template on legacy docs missing the field
        if (entity.getRmpFormTemplate() == null) {
            entity.setRmpFormTemplate(existingTemplate);
        }
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public MaterialMasterRecordDto softDisable(MaterialMasterDeleteRequest request) {
        MaterialMaster entity = requireByMaterialId(request.getMaterialId());
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public MaterialMasterRecordDto enable(MaterialMasterDeleteRequest request) {
        MaterialMaster entity = requireByMaterialId(request.getMaterialId());
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public void requireActiveMaterial(MaterialMaster material) {
        if (!isActive(material)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Material is inactive: " + material.getMaterialCode());
        }
    }

    public static RawMaterialCategory resolveRawMaterialCategory(RawMaterialCategory rawMaterialType) {
        return rawMaterialType == null ? RawMaterialCategory.NORMAL : rawMaterialType;
    }

    /**
     * Resolved RMP form template for a material.
     * Missing field → DEFAULT (default solid/liquid UI by material type). No materialCode guessing.
     */
    public static RmpFormTemplate resolveRmpFormTemplate(MaterialMaster material) {
        if (material == null || material.getRmpFormTemplate() == null) {
            return RmpFormTemplate.DEFAULT;
        }
        return material.getRmpFormTemplate();
    }

    private RmpFormTemplate validateRmpFormTemplate(
            RmpFormTemplate requested,
            MaterialType materialType,
            List<MaterialGrade> grades) {
        RmpFormTemplate template = RmpFormTemplate.orDefault(requested);
        if (materialType == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "materialType is required");
        }
        boolean hasGrades = grades != null && !grades.isEmpty();
        switch (template) {
            case DEFAULT -> {
                // always allowed
            }
            case AP, ALUMINUM -> {
                if (materialType != MaterialType.SOLID) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            template + " form template requires materialType SOLID");
                }
                if (template == RmpFormTemplate.AP && !hasGrades) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            "AP form template requires at least one grade");
                }
            }
            case DOA, HTPB, TDI -> {
                if (materialType != MaterialType.LIQUID) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            template + " form template requires materialType LIQUID");
                }
            }
            case CC, IO, NONOX_D -> {
                if (materialType != MaterialType.SOLID) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            template + " form template requires materialType SOLID");
                }
            }
        }
        return template;
    }

    private PreparationType validateRawMaterialClassification(
            RawMaterialCategory rawMaterialType,
            PreparationType preparationType) {
        if (rawMaterialType == RawMaterialCategory.ACEM) {
            if (preparationType == null) {
                throw new AcemException(ErrorCode.VALIDATION_ERROR,
                        "preparationType is required for ACEM raw materials");
            }
            return preparationType;
        }
        if (preparationType != null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR,
                    "preparationType is only allowed for ACEM raw materials");
        }
        return null;
    }

    private void validateTree(List<MaterialGrade> grades, List<MaterialSpecification> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Material must have at least one top-level specification");
        }
    }

    private List<MaterialGrade> mapGrades(
            List<MaterialGradeWriteDto> grades,
            List<MaterialGrade> existingGrades,
            AtomicLong specCodeCounter) {
        if (grades == null || grades.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, MaterialGrade> existingByCode = indexGradesByCode(existingGrades);
        Set<String> codes = new HashSet<>();
        long nextGradeId = 1L;
        List<MaterialGrade> result = new ArrayList<>();
        for (MaterialGradeWriteDto g : grades) {
            String gradeCode = MasterDataValidation.validateCode(g.getGradeCode());
            if (!codes.add(gradeCode)) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Duplicate grade code: " + gradeCode);
            }
            MaterialGrade existingGrade = existingByCode.get(gradeCode.toLowerCase(Locale.ROOT));
            String gradeId = g.getGradeId();
            if (gradeId == null || gradeId.isBlank()) {
                gradeId = existingGrade != null && existingGrade.getGradeId() != null
                        ? existingGrade.getGradeId()
                        : String.valueOf(nextGradeId++);
            } else {
                try {
                    nextGradeId = Math.max(nextGradeId, Long.parseLong(gradeId) + 1);
                } catch (NumberFormatException ignored) {
                    nextGradeId++;
                }
            }
            List<MaterialSpecification> gradeSpecs = existingGrade == null
                    ? mapSpecsForCreate(g.getSpecifications(), specCodeCounter)
                    : mapSpecsForUpdate(
                            g.getSpecifications(),
                            existingGrade.getSpecifications(),
                            specCodeCounter);
            result.add(MaterialGrade.builder()
                    .gradeId(gradeId)
                    .gradeCode(gradeCode)
                    .gradeName(MasterDataValidation.validateName(g.getGradeName()))
                    .specifications(gradeSpecs)
                    .build());
        }
        return result;
    }

    private List<MaterialSpecification> mapSpecsForCreate(
            List<MaterialSpecWriteDto> specs,
            AtomicLong specCodeCounter) {
        if (specs == null || specs.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> codes = new HashSet<>();
        List<MaterialSpecification> result = new ArrayList<>();
        for (MaterialSpecWriteDto s : specs) {
            result.add(buildSpecification(s, specCodeCounter, codes, null));
        }
        return result;
    }

    private List<MaterialSpecification> mapSpecsForUpdate(
            List<MaterialSpecWriteDto> incoming,
            List<MaterialSpecification> existing,
            AtomicLong specCodeCounter) {
        if (incoming == null || incoming.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, MaterialSpecification> existingByCode = indexSpecificationsByCode(existing);
        Set<String> codes = new HashSet<>();
        List<MaterialSpecification> result = new ArrayList<>();
        for (MaterialSpecWriteDto s : incoming) {
            String code = resolveSpecificationCode(s.getSpecificationCode(), specCodeCounter);
            MaterialSpecification existingSpec = existingByCode.get(code.toLowerCase(Locale.ROOT));
            result.add(buildSpecification(s, specCodeCounter, codes, existingSpec));
        }
        return result;
    }

    private MaterialSpecification buildSpecification(
            MaterialSpecWriteDto s,
            AtomicLong specCodeCounter,
            Set<String> codes,
            MaterialSpecification existingSpec) {
        String code = resolveSpecificationCode(s.getSpecificationCode(), specCodeCounter);
        if (!codes.add(code)) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Duplicate specification code: " + code);
        }
        if (existingSpec == null) {
            ReferenceRange range = resolveReferenceRange(s.getReferenceRange());
            validateRange(code, range);
            return MaterialSpecification.builder()
                    .specificationCode(code)
                    .specificationName(MasterDataValidation.validateName(s.getSpecificationName()))
                    .referenceRange(range)
                    .isActive(resolveNestedActive(s.getIsActive()))
                    .build();
        }
        assertExistingSpecificationUnchanged(existingSpec, s, code);
        return MaterialSpecification.builder()
                .specificationCode(existingSpec.getSpecificationCode())
                .specificationName(existingSpec.getSpecificationName())
                .referenceRange(existingSpec.getReferenceRange())
                .isActive(resolveNestedActive(s.getIsActive()))
                .build();
    }

    private static void validateRange(String code, ReferenceRange range) {
        if (range != null && range.getMinValue() != null && range.getMaxValue() != null
                && range.getMinValue() > range.getMaxValue()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Specification minValue cannot exceed maxValue: " + code);
        }
    }

    private static void assertExistingSpecificationUnchanged(
            MaterialSpecification existingSpec,
            MaterialSpecWriteDto incoming,
            String code) {
        String incomingName = incoming.getSpecificationName() == null
                ? ""
                : incoming.getSpecificationName().trim();
        if (!incomingName.isBlank()
                && existingSpec.getSpecificationName() != null
                && !existingSpec.getSpecificationName().equals(incomingName)) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing specification name cannot be changed: " + code);
        }
    }

    private static boolean resolveNestedActive(Boolean isActive) {
        return isActive == null || Boolean.TRUE.equals(isActive);
    }

    private static Map<String, MaterialSpecification> indexSpecificationsByCode(
            List<MaterialSpecification> specifications) {
        Map<String, MaterialSpecification> indexed = new LinkedHashMap<>();
        if (specifications == null) {
            return indexed;
        }
        for (MaterialSpecification specification : specifications) {
            if (specification == null || specification.getSpecificationCode() == null) {
                continue;
            }
            indexed.putIfAbsent(
                    specification.getSpecificationCode().trim().toLowerCase(Locale.ROOT),
                    specification);
        }
        return indexed;
    }

    private static Map<String, MaterialGrade> indexGradesByCode(List<MaterialGrade> grades) {
        Map<String, MaterialGrade> indexed = new HashMap<>();
        if (grades == null) {
            return indexed;
        }
        for (MaterialGrade grade : grades) {
            if (grade == null || grade.getGradeCode() == null) {
                continue;
            }
            indexed.putIfAbsent(grade.getGradeCode().trim().toLowerCase(Locale.ROOT), grade);
        }
        return indexed;
    }

    private String resolveSpecificationCode(String rawCode, AtomicLong specCodeCounter) {
        if (rawCode == null || rawCode.isBlank()) {
            return MasterDataCatalog.generatedCode(
                    MasterDataCodePrefixes.SPEC,
                    specCodeCounter.getAndIncrement());
        }
        return MasterDataValidation.validateCode(rawCode);
    }

    private ReferenceRange resolveReferenceRange(ReferenceRange range) {
        if (range == null) {
            return null;
        }
        if (range.getUnitId() != null) {
            UnitMaster unit = unitMasterRepository.findFirstById(range.getUnitId()).orElse(null);
            if (unit != null) {
                return ReferenceRange.builder()
                        .minValue(range.getMinValue())
                        .maxValue(range.getMaxValue())
                        .unitId(range.getUnitId())
                        .unit(resolveUnitDisplay(unit))
                        .build();
            }
            if (range.getUnit() != null && !range.getUnit().isBlank()) {
                return ReferenceRange.builder()
                        .minValue(range.getMinValue())
                        .maxValue(range.getMaxValue())
                        .unitId(range.getUnitId())
                        .unit(range.getUnit().trim())
                        .build();
            }
            throw new AcemException(ErrorCode.VALIDATION_ERROR,
                    "Invalid unitId: " + range.getUnitId());
        }
        if (range.getUnit() != null && !range.getUnit().isBlank()) {
            return ReferenceRange.builder()
                    .minValue(range.getMinValue())
                    .maxValue(range.getMaxValue())
                    .unit(range.getUnit().trim())
                    .build();
        }
        return ReferenceRange.builder()
                .minValue(range.getMinValue())
                .maxValue(range.getMaxValue())
                .build();
    }

    private static String resolveUnitDisplay(UnitMaster unit) {
        if (unit.getSymbol() != null && !unit.getSymbol().isBlank()) {
            return unit.getSymbol().trim();
        }
        if (unit.getUnitCode() != null && !unit.getUnitCode().isBlank()) {
            return unit.getUnitCode().trim();
        }
        if (unit.getName() != null && !unit.getName().isBlank()) {
            return unit.getName().trim();
        }
        return String.valueOf(unit.getId());
    }

    private MaterialMasterRecordDto toDto(MaterialMaster entity) {
        return MaterialMasterRecordDto.builder()
                .materialId(entity.getMaterialId())
                .materialCode(entity.getMaterialCode())
                .materialName(entity.getMaterialName())
                .materialType(entity.getMaterialType())
                .rawMaterialType(resolveRawMaterialCategory(entity.getRawMaterialType()))
                .preparationType(entity.getPreparationType())
                .rmpFormTemplate(resolveRmpFormTemplate(entity))
                .isActive(isActive(entity))
                .grades(entity.getGrades() == null ? List.of() : entity.getGrades().stream()
                        .map(g -> MaterialGradeDto.builder()
                                .gradeId(g.getGradeId())
                                .gradeCode(g.getGradeCode())
                                .gradeName(g.getGradeName())
                                .specifications(mapSpecDtos(g.getSpecifications()))
                                .build())
                        .toList())
                .specifications(mapSpecDtos(entity.getSpecifications()))
                .createdBy(MasterDataValidation.auditCreatedBy(entity))
                .createdOn(MasterDataValidation.auditCreatedOn(entity))
                .updatedBy(MasterDataValidation.auditUpdatedBy(entity))
                .updatedOn(MasterDataValidation.auditUpdatedOn(entity))
                .build();
    }

    private List<MaterialSpecDto> mapSpecDtos(List<MaterialSpecification> specs) {
        if (specs == null) return List.of();
        return specs.stream()
                .map(s -> MaterialSpecDto.builder()
                        .specificationCode(s.getSpecificationCode())
                        .specificationName(s.getSpecificationName())
                        .referenceRange(s.getReferenceRange())
                        .isActive(isSpecificationActive(s))
                        .build())
                .toList();
    }

    private boolean matchesActive(MaterialMaster m, Boolean filter) {
        if (filter == null) return true;
        return filter == isActive(m);
    }

    private boolean matchesType(MaterialMaster m, String materialType) {
        if (materialType == null || materialType.isBlank() || "BOTH".equalsIgnoreCase(materialType)) {
            return true;
        }
        try {
            return m.getMaterialType() == MaterialType.valueOf(materialType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "materialType must be SOLID, LIQUID or BOTH");
        }
    }

    private boolean matchesSearch(MaterialMaster m, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase(Locale.ROOT);
        return (m.getMaterialCode() != null && m.getMaterialCode().toLowerCase(Locale.ROOT).contains(q))
                || (m.getMaterialName() != null && m.getMaterialName().toLowerCase(Locale.ROOT).contains(q));
    }

    private boolean matchesRawMaterialCategory(MaterialMaster m, String rawMaterialType) {
        if (rawMaterialType == null || rawMaterialType.isBlank()) {
            return true;
        }
        try {
            return resolveRawMaterialCategory(m.getRawMaterialType())
                    == RawMaterialCategory.fromValue(rawMaterialType);
        } catch (IllegalArgumentException ex) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "rawMaterialType must be NORMAL or ACEM");
        }
    }

    private boolean matchesPreparationType(MaterialMaster m, String preparationType) {
        if (preparationType == null || preparationType.isBlank()) {
            return true;
        }
        try {
            PreparationType filter = PreparationType.fromValue(preparationType);
            return Objects.equals(m.getPreparationType(), filter);
        } catch (IllegalArgumentException ex) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "preparationType must be ADDUCT or HTPB Blending");
        }
    }

    private MaterialMaster requireByMaterialId(Long materialId) {
        if (materialId == null) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "materialId is required");
        }
        return repository.findFirstByMaterialId(materialId)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND, "Material not found"));
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(MaterialMaster::getMaterialId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }
}
