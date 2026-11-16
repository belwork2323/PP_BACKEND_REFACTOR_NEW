package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataCreateRequest;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.dto.masterdata.MasterDataUpdateRequest;
import in.co.bel.acempp.entity.DimensionalParameter;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.DimensionalParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DimensionalParameterMasterDataHandler implements MasterDataTypeHandler {

    private final DimensionalParameterRepository repository;

    @Override
    public String type() {
        return MasterDataCatalog.DIMENSIONAL_PARAMETERS;
    }

    @Override
    public List<MasterDataRecordDto> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator
                        .comparing(DimensionalParameter::getProjectId, Comparator.nullsLast(String::compareTo))
                        .thenComparing(DimensionalParameter::getMotorType, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DimensionalParameter::getParamId, Comparator.nullsLast(String::compareTo)))
                .map(this::toDto)
                .toList();
    }

    @Override
    public MasterDataRecordDto create(MasterDataCreateRequest request) {
        MasterDataTypeDescriptor schema = MasterDataCatalog.get(type());
        String name = MasterDataValidation.validateName(request.getName());
        Map<String, Object> attrs = MasterDataValidation.validateAttributes(schema, request.getAttributes());

        String projectId = requireProjectId(attrs.get("projectId"));
        Integer motorType = requireMotorType(attrs.get("motorType"));
        boolean willBeActive = request.getIsActive() == null || Boolean.TRUE.equals(request.getIsActive());
        if (willBeActive) {
            assertNoOtherActiveWithSameName(projectId, motorType, name, null);
        }

        String paramId = (String) attrs.get("paramId");
        if (paramId == null || paramId.isBlank()) {
            paramId = resolveNextParamId(projectId, motorType);
        }
        Double minValue = toDouble(attrs.get("minValue"));
        Double maxValue = toDouble(attrs.get("maxValue"));
        String unit = (String) attrs.get("unit");

        if (minValue != null && maxValue != null && minValue > maxValue) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "minValue cannot exceed maxValue");
        }
        if (willBeActive) {
            assertNoOtherActiveWithSameParamId(projectId, motorType, paramId, null);
        }

        DimensionalParameter entity = DimensionalParameter.builder()
                .parameterId(nextId())
                .projectId(projectId)
                .motorType(motorType)
                .paramId(paramId)
                .paramName(name)
                .minValue(minValue)
                .maxValue(maxValue)
                .unit(unit)
                .isActive(willBeActive)
                .build();
        MasterDataAuditSupport.stampOnCreate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto update(MasterDataUpdateRequest request) {
        DimensionalParameter entity = repository.findFirstByParameterId(request.getId())
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND,
                        "Dimensional parameter not found"));
        boolean enabling = Boolean.TRUE.equals(request.getIsActive()) && !isActive(entity);
        if (enabling) {
            assertCanEnable(entity);
        }
        entity.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto softDisable(Long id) {
        DimensionalParameter entity = repository.findFirstByParameterId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND,
                        "Dimensional parameter not found"));
        entity.setIsActive(false);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    @Override
    public MasterDataRecordDto enable(Long id) {
        DimensionalParameter entity = repository.findFirstByParameterId(id)
                .orElseThrow(() -> new AcemException(ErrorCode.MASTER_DATA_NOT_FOUND,
                        "Dimensional parameter not found"));
        if (isActive(entity)) {
            return toDto(entity);
        }
        assertCanEnable(entity);
        entity.setIsActive(true);
        MasterDataAuditSupport.stampOnUpdate(entity);
        return toDto(repository.save(entity));
    }

    public static boolean isActive(DimensionalParameter param) {
        return MasterDataValidation.isActiveFlag(param.getIsActive());
    }

    private void assertCanEnable(DimensionalParameter entity) {
        String projectId = requireProjectId(entity.getProjectId());
        Integer motorType = requireMotorType(entity.getMotorType());
        String name = MasterDataValidation.requireTrimmed(entity.getParamName(), "Parameter name");
        assertNoOtherActiveWithSameName(projectId, motorType, name, entity.getParameterId());
        assertNoOtherActiveWithSameParamId(
                projectId, motorType, entity.getParamId(), entity.getParameterId());
    }

    /**
     * Active uniqueness for (projectId, motorType, paramName). Scans all docs so query
     * type mismatches cannot skip the conflict.
     */
    private void assertNoOtherActiveWithSameName(
            String projectId, Integer motorType, String name, Long excludeParameterId) {
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        String normalizedProject = projectId.trim();
        boolean duplicate = repository.findAll().stream()
                .filter(DimensionalParameterMasterDataHandler::isActive)
                .filter(existing -> excludeParameterId == null
                        || !Objects.equals(existing.getParameterId(), excludeParameterId))
                .filter(existing -> normalizedProject.equals(trimToEmpty(existing.getProjectId())))
                .filter(existing -> Objects.equals(motorType, existing.getMotorType()))
                .anyMatch(existing -> existing.getParamName() != null
                        && normalizedName.equals(existing.getParamName().trim().toLowerCase(Locale.ROOT)));
        if (duplicate) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_NAME,
                    "Parameter already exists and is active for this project and motor stage "
                            + motorType + ": " + name.trim());
        }
    }

    private void assertNoOtherActiveWithSameParamId(
            String projectId, Integer motorType, String paramId, Long excludeParameterId) {
        if (paramId == null || paramId.isBlank()) {
            return;
        }
        String normalizedParamId = paramId.trim();
        String normalizedProject = projectId.trim();
        boolean duplicate = repository.findAll().stream()
                .filter(DimensionalParameterMasterDataHandler::isActive)
                .filter(existing -> excludeParameterId == null
                        || !Objects.equals(existing.getParameterId(), excludeParameterId))
                .filter(existing -> normalizedProject.equals(trimToEmpty(existing.getProjectId())))
                .filter(existing -> Objects.equals(motorType, existing.getMotorType()))
                .anyMatch(existing -> existing.getParamId() != null
                        && normalizedParamId.equals(existing.getParamId().trim()));
        if (duplicate) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                    "Parameter already exists and is active for this project and motor stage "
                            + motorType + " (paramId " + normalizedParamId + ")");
        }
    }

    private static Integer requireMotorType(Object raw) {
        if (raw == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Motor stage is required");
        }
        if (raw instanceof Integer integer) {
            return integer;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Motor stage is required");
        }
    }

    private static String requireProjectId(Object raw) {
        if (raw == null) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Project is required");
        }
        String projectId = String.valueOf(raw).trim();
        if (projectId.isEmpty()) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Project is required");
        }
        return projectId;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.doubleValue();
        return Double.valueOf(value.toString());
    }

    private long nextId() {
        return repository.findAll().stream()
                .map(DimensionalParameter::getParameterId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private String resolveNextParamId(String projectId, Integer motorType) {
        String prefix = MasterDataCodePrefixes.DIMENSIONAL_PARAM + "-" + motorType + "-";
        int maxSeq = repository.findByProjectIdAndMotorType(projectId, motorType).stream()
                .map(DimensionalParameter::getParamId)
                .filter(Objects::nonNull)
                .mapToInt(id -> {
                    if (!id.startsWith(prefix)) {
                        return 0;
                    }
                    String suffix = id.substring(prefix.length());
                    try {
                        return Integer.parseInt(suffix);
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return prefix + String.format("%03d", maxSeq + 1);
    }

    private MasterDataRecordDto toDto(DimensionalParameter entity) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("projectId", entity.getProjectId());
        attrs.put("motorType", entity.getMotorType());
        attrs.put("paramId", entity.getParamId());
        attrs.put("minValue", entity.getMinValue());
        attrs.put("maxValue", entity.getMaxValue());
        attrs.put("unit", entity.getUnit());
        return MasterDataValidation.record(
                entity.getParameterId(),
                entity.getParamId(),
                entity.getParamName(),
                entity.getIsActive(),
                attrs, entity);
    }
}
