package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataStatsDto;
import in.co.bel.acempp.dto.masterdata.insulation.*;
import in.co.bel.acempp.dto.rawmaterialsourcing.ReferenceRange;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.CategoryDto;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.ParameterDto;
import in.co.bel.acempp.entity.InsulationSpecification;
import in.co.bel.acempp.entity.batch.UnitMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.InsulationSpecificationRepository;
import in.co.bel.acempp.repository.UnitMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class InsulationSpecificationMasterDataService {

    private final InsulationSpecificationRepository repository;
    private final UnitMasterRepository unitMasterRepository;

    public static boolean isActive(InsulationSpecification entity) {
        return MasterDataValidation.isActiveFlag(entity.getIsActive());
    }

    public InsulationSpecListResponse list(InsulationSpecListRequest request) {
        InsulationSpecListRequest req = request == null ? new InsulationSpecListRequest() : request;
        List<InsulationSpecification> all = repository.findAll().stream()
                .sorted(MasterDataListOrdering.byLongIdDesc(InsulationSpecification::getInsulationSpecId))
                .toList();

        long active = all.stream().filter(InsulationSpecificationMasterDataService::isActive).count();
        long inactive = all.size() - active;

        List<InsulationSpecRecordDto> items = all.stream()
                .filter(e -> matchesActive(e, req.getIsActive()))
                .filter(e -> matchesSearch(e, req.getSearch()))
                .map(this::toDto)
                .toList();

        return InsulationSpecListResponse.builder()
                .items(items)
                .stats(MasterDataStatsDto.builder()
                        .total(all.size())
                        .active(active)
                        .inactive(inactive)
                        .build())
                .build();
    }

    public InsulationSpecRecordDto create(InsulationSpecCreateRequest request) {
        String insulationType = MasterDataValidation.validateInsulationType(request.getInsulationType());
        if (repository.findByInsulationType(insulationType).isPresent()) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                    "Insulation specification already exists for type: " + insulationType);
        }
        AtomicLong specCodeCounter = new AtomicLong(1L);
        List<CategoryDto> specs = mapCategories(request.getSpecifications(), specCodeCounter);
        long insulationSpecId = nextInsulationSpecId();
        String code = resolveUniqueCode(insulationSpecId);
        InsulationSpecification entity = InsulationSpecification.builder()
                .insulationSpecId(insulationSpecId)
                .code(code)
                .insulationType(insulationType)
                .specifications(specs)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    public InsulationSpecRecordDto update(InsulationSpecUpdateRequest request) {
        InsulationSpecification entity = requireByInsulationSpecId(request.getInsulationSpecId());
        if (request.getInsulationType() != null
                && !request.getInsulationType().equals(entity.getInsulationType())) {
            throw new AcemException(ErrorCode.READONLY_FIELD, "Insulation type cannot be changed");
        }
        AtomicLong specCodeCounter = new AtomicLong(nextSpecCodeSequence(entity.getSpecifications()));
        entity.setSpecifications(mapCategoriesForUpdate(
                request.getSpecifications(),
                entity.getSpecifications(),
                specCodeCounter));
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public InsulationSpecRecordDto softDisable(InsulationSpecDeleteRequest request) {
        InsulationSpecification entity = requireByInsulationSpecId(request.getInsulationSpecId());
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public void requireActive(InsulationSpecification entity) {
        if (!isActive(entity)) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Insulation specification is inactive: " + entity.getInsulationType());
        }
    }

    private List<CategoryDto> mapCategories(List<CategoryDto> categories, AtomicLong specCodeCounter) {
        if (categories == null || categories.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "At least one category with parameters is required");
        }
        Set<String> categoryNames = new HashSet<>();
        List<CategoryDto> result = new ArrayList<>();
        for (CategoryDto cat : categories) {
            String category = MasterDataValidation.requireTrimmed(cat.getCategory(), "Category");
            if (!categoryNames.add(category.toLowerCase(Locale.ROOT))) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Duplicate category: " + category);
            }
            result.add(CategoryDto.builder()
                    .category(category)
                    .parameters(mapParameters(cat.getParameters(), specCodeCounter))
                    .isActive(resolveNestedActive(cat.getIsActive()))
                    .build());
        }
        return result;
    }

    private List<CategoryDto> mapCategoriesForUpdate(
            List<CategoryDto> incoming,
            List<CategoryDto> existing,
            AtomicLong specCodeCounter) {
        if (incoming == null || incoming.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "At least one category with parameters is required");
        }
        Map<String, CategoryDto> existingByName = indexCategoriesByName(existing);
        Set<String> categoryNames = new HashSet<>();
        List<CategoryDto> result = new ArrayList<>();
        for (CategoryDto cat : incoming) {
            String category = MasterDataValidation.requireTrimmed(cat.getCategory(), "Category");
            String categoryKey = category.toLowerCase(Locale.ROOT);
            if (!categoryNames.add(categoryKey)) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Duplicate category: " + category);
            }
            CategoryDto existingCategory = existingByName.get(categoryKey);
            if (existingCategory == null) {
                result.add(CategoryDto.builder()
                        .category(category)
                        .parameters(mapParameters(cat.getParameters(), specCodeCounter))
                        .isActive(resolveNestedActive(cat.getIsActive()))
                        .build());
                continue;
            }
            if (!category.equals(existingCategory.getCategory())) {
                throw new AcemException(ErrorCode.READONLY_FIELD,
                        "Existing category name cannot be changed: " + existingCategory.getCategory());
            }
            result.add(CategoryDto.builder()
                    .category(category)
                    .parameters(mapParametersForUpdate(
                            cat.getParameters(),
                            existingCategory.getParameters(),
                            specCodeCounter))
                    .isActive(resolveNestedActive(cat.getIsActive()))
                    .build());
        }
        return result;
    }

    private List<ParameterDto> mapParameters(List<ParameterDto> parameters, AtomicLong specCodeCounter) {
        if (parameters == null || parameters.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Each category must have at least one parameter");
        }
        Set<String> codes = new HashSet<>();
        List<ParameterDto> result = new ArrayList<>();
        for (ParameterDto p : parameters) {
            String code = resolveSpecificationCode(p.getSpecificationCode(), specCodeCounter);
            if (!codes.add(code)) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                        "Duplicate specification code: " + code);
            }
            ReferenceRange range = p.getReferenceRange();
            if (range != null && range.getMinValue() != null && range.getMaxValue() != null
                    && range.getMinValue() > range.getMaxValue()) {
                throw new AcemException(ErrorCode.INVALID_REQUEST,
                        "Specification minValue cannot exceed maxValue: " + code);
            }
            result.add(ParameterDto.builder()
                    .specificationCode(code)
                    .specificationName(MasterDataValidation.validateName(p.getSpecificationName()))
                    .referenceRange(resolveReferenceRange(range, true))
                    .isActive(resolveNestedActive(p.getIsActive()))
                    .build());
        }
        return result;
    }

    private List<ParameterDto> mapParametersForUpdate(
            List<ParameterDto> incoming,
            List<ParameterDto> existing,
            AtomicLong specCodeCounter) {
        if (incoming == null || incoming.isEmpty()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "Each category must have at least one parameter");
        }
        Map<String, ParameterDto> existingByCode = indexParametersByCode(existing);
        Set<String> codes = new HashSet<>();
        List<ParameterDto> result = new ArrayList<>();
        for (ParameterDto p : incoming) {
            String code = resolveSpecificationCode(p.getSpecificationCode(), specCodeCounter);
            if (!codes.add(code)) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                        "Duplicate specification code: " + code);
            }
            ParameterDto existingParam = existingByCode.get(code.toLowerCase(Locale.ROOT));
            if (existingParam == null) {
                ReferenceRange range = p.getReferenceRange();
                if (range != null && range.getMinValue() != null && range.getMaxValue() != null
                        && range.getMinValue() > range.getMaxValue()) {
                    throw new AcemException(ErrorCode.INVALID_REQUEST,
                            "Specification minValue cannot exceed maxValue: " + code);
                }
                result.add(ParameterDto.builder()
                        .specificationCode(code)
                        .specificationName(MasterDataValidation.validateName(p.getSpecificationName()))
                        .referenceRange(resolveReferenceRange(range, true))
                        .isActive(resolveNestedActive(p.getIsActive()))
                        .build());
                continue;
            }
            assertExistingParameterUnchanged(existingParam, p, code);
            result.add(ParameterDto.builder()
                    .specificationCode(existingParam.getSpecificationCode())
                    .specificationName(existingParam.getSpecificationName())
                    .referenceRange(existingParam.getReferenceRange())
                    .isActive(resolveNestedActive(p.getIsActive()))
                    .build());
        }
        return result;
    }

    private String resolveSpecificationCode(String rawCode, AtomicLong specCodeCounter) {
        if (rawCode == null || rawCode.isBlank()) {
            return MasterDataCatalog.generatedCode(
                    MasterDataCodePrefixes.SPEC,
                    specCodeCounter.getAndIncrement());
        }
        return MasterDataValidation.validateCode(rawCode);
    }

    private List<CategoryDto> mapSpecificationsForRead(List<CategoryDto> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        return categories.stream()
                .map(cat -> CategoryDto.builder()
                        .category(cat.getCategory())
                        .parameters(cat.getParameters() == null ? List.of() : cat.getParameters().stream()
                                .map(p -> ParameterDto.builder()
                                        .specificationCode(p.getSpecificationCode())
                                        .specificationName(p.getSpecificationName())
                                        .referenceRange(resolveReferenceRange(p.getReferenceRange(), false))
                                        .isActive(resolveNestedActive(p.getIsActive()))
                                        .build())
                                .toList())
                        .isActive(resolveNestedActive(cat.getIsActive()))
                        .build())
                .toList();
    }

    private static boolean resolveNestedActive(Boolean isActive) {
        return isActive == null || Boolean.TRUE.equals(isActive);
    }

    public static boolean isCategoryActive(CategoryDto category) {
        return category != null
                && (category.getIsActive() == null || Boolean.TRUE.equals(category.getIsActive()));
    }

    public static boolean isParameterActive(ParameterDto parameter) {
        return parameter != null
                && (parameter.getIsActive() == null || Boolean.TRUE.equals(parameter.getIsActive()));
    }

    public static List<CategoryDto> filterActiveSpecifications(List<CategoryDto> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        return categories.stream()
                .filter(InsulationSpecificationMasterDataService::isCategoryActive)
                .map(cat -> CategoryDto.builder()
                        .category(cat.getCategory())
                        .parameters(cat.getParameters() == null
                                ? List.of()
                                : cat.getParameters().stream()
                                        .filter(InsulationSpecificationMasterDataService::isParameterActive)
                                        .toList())
                        .build())
                .filter(cat -> cat.getParameters() != null && !cat.getParameters().isEmpty())
                .toList();
    }

    private static Map<String, CategoryDto> indexCategoriesByName(List<CategoryDto> categories) {
        Map<String, CategoryDto> indexed = new HashMap<>();
        if (categories == null) {
            return indexed;
        }
        for (CategoryDto category : categories) {
            if (category.getCategory() == null || category.getCategory().isBlank()) {
                continue;
            }
            indexed.put(category.getCategory().trim().toLowerCase(Locale.ROOT), category);
        }
        return indexed;
    }

    private static Map<String, ParameterDto> indexParametersByCode(List<ParameterDto> parameters) {
        Map<String, ParameterDto> indexed = new HashMap<>();
        if (parameters == null) {
            return indexed;
        }
        for (ParameterDto parameter : parameters) {
            if (parameter.getSpecificationCode() == null || parameter.getSpecificationCode().isBlank()) {
                continue;
            }
            indexed.put(parameter.getSpecificationCode().trim().toLowerCase(Locale.ROOT), parameter);
        }
        return indexed;
    }

    private static void assertExistingParameterUnchanged(
            ParameterDto existing,
            ParameterDto incoming,
            String code) {
        String existingName = existing.getSpecificationName() == null ? "" : existing.getSpecificationName().trim();
        String incomingName = incoming.getSpecificationName() == null ? "" : incoming.getSpecificationName().trim();
        if (!existingName.equals(incomingName)) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing parameter name cannot be changed: " + code);
        }
        if (!referenceRangesEqual(existing.getReferenceRange(), incoming.getReferenceRange())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing parameter range cannot be changed: " + code);
        }
    }

    private static boolean referenceRangesEqual(ReferenceRange left, ReferenceRange right) {
        ReferenceRange a = left == null ? ReferenceRange.builder().build() : left;
        ReferenceRange b = right == null ? ReferenceRange.builder().build() : right;
        return Objects.equals(a.getMinValue(), b.getMinValue())
                && Objects.equals(a.getMaxValue(), b.getMaxValue())
                && Objects.equals(a.getUnitId(), b.getUnitId());
    }

    private static long nextSpecCodeSequence(List<CategoryDto> categories) {
        long max = 0L;
        if (categories == null) {
            return 1L;
        }
        String prefix = MasterDataCodePrefixes.SPEC + "-";
        for (CategoryDto category : categories) {
            if (category.getParameters() == null) {
                continue;
            }
            for (ParameterDto parameter : category.getParameters()) {
                String code = parameter.getSpecificationCode();
                if (code == null || !code.startsWith(prefix)) {
                    continue;
                }
                try {
                    max = Math.max(max, Long.parseLong(code.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // ignore non-numeric generated codes
                }
            }
        }
        return max + 1L;
    }

    private ReferenceRange resolveReferenceRange(ReferenceRange range, boolean validateUnit) {
        if (range == null) {
            return null;
        }
        if (range.getUnitId() != null) {
            Optional<UnitMaster> unitOpt = unitMasterRepository.findFirstById(range.getUnitId());
            if (unitOpt.isEmpty()) {
                if (validateUnit) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            "Invalid unitId: " + range.getUnitId());
                }
                return ReferenceRange.builder()
                        .minValue(range.getMinValue())
                        .maxValue(range.getMaxValue())
                        .unitId(range.getUnitId())
                        .build();
            }
            UnitMaster unit = unitOpt.get();
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

    private InsulationSpecification requireByInsulationSpecId(Long insulationSpecId) {
        if (insulationSpecId == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "insulationSpecId is required");
        }
        return repository.findFirstByInsulationSpecId(insulationSpecId)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND,
                        "Insulation specification not found"));
    }

    private long nextInsulationSpecId() {
        return repository.findAll().stream()
                .map(InsulationSpecification::getInsulationSpecId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private String resolveUniqueCode(long startId) {
        long id = startId;
        while (true) {
            String code = MasterDataCatalog.generatedCode(MasterDataCodePrefixes.INSULATION, id);
            if (repository.findByCode(code).isEmpty()) {
                return code;
            }
            id++;
        }
    }

    private InsulationSpecRecordDto toDto(InsulationSpecification entity) {
        return InsulationSpecRecordDto.builder()
                .insulationSpecId(entity.getInsulationSpecId())
                .code(entity.getCode())
                .insulationType(entity.getInsulationType())
                .isActive(isActive(entity))
                .specifications(mapSpecificationsForRead(entity.getSpecifications()))
                .createdBy(MasterDataValidation.auditCreatedBy(entity))
                .createdOn(MasterDataValidation.auditCreatedOn(entity))
                .updatedBy(MasterDataValidation.auditUpdatedBy(entity))
                .updatedOn(MasterDataValidation.auditUpdatedOn(entity))
                .build();
    }

    private boolean matchesActive(InsulationSpecification e, Boolean filter) {
        if (filter == null) return true;
        return filter == isActive(e);
    }

    private boolean matchesSearch(InsulationSpecification e, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase(Locale.ROOT);
        if (e.getCode() != null && e.getCode().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        return e.getInsulationType() != null
                && e.getInsulationType().toLowerCase(Locale.ROOT).contains(q);
    }
}
