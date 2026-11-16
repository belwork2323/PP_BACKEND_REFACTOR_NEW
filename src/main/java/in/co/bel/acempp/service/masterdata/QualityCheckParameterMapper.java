package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.dto.user.operations.SpecificationRangeDto;
import in.co.bel.acempp.entity.batch.UnitMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.UnitMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared create/update mapping for nested quality-check parameters. */
@Component
@RequiredArgsConstructor
public class QualityCheckParameterMapper {

    private final UnitMasterRepository unitMasterRepository;

    public static boolean isParameterActive(QualityCheckParams parameter) {
        return parameter == null || parameter.getIsActive() == null || Boolean.TRUE.equals(parameter.getIsActive());
    }

    public static List<QualityCheckParams> activeParameters(List<QualityCheckParams> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return List.of();
        }
        return parameters.stream()
                .filter(QualityCheckParameterMapper::isParameterActive)
                .map(QualityCheckParameterMapper::toParameterDto)
                .toList();
    }

    /** Create mapping — empty list allowed when {@code requireAtLeastOne} is false. */
    public List<QualityCheckParams> mapChecks(List<QualityCheckParams> checks, boolean requireAtLeastOne) {
        if (checks == null || checks.isEmpty()) {
            if (requireAtLeastOne) {
                throw new AcemException(ErrorCode.INVALID_REQUEST, "At least one quality check parameter is required");
            }
            return new ArrayList<>();
        }
        Set<String> ids = new HashSet<>();
        List<QualityCheckParams> result = new ArrayList<>();
        int nextId = 1;
        for (QualityCheckParams check : checks) {
            String parameterId = check.getParameterId();
            if (parameterId == null || parameterId.isBlank()) {
                parameterId = String.valueOf(nextId++);
            } else {
                parameterId = parameterId.trim();
                try {
                    nextId = Math.max(nextId, Integer.parseInt(parameterId) + 1);
                } catch (NumberFormatException ignored) {
                    nextId++;
                }
            }
            if (!ids.add(parameterId)) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                        "Duplicate parameterId: " + parameterId);
            }
            result.add(buildParameter(check, parameterId));
        }
        return result;
    }

    /**
     * Update mapping — empty incoming allowed when {@code requireAtLeastOne} is false and existing is empty.
     * Existing params cannot be removed; new params may be appended.
     */
    public List<QualityCheckParams> mapChecksForUpdate(
            List<QualityCheckParams> incoming,
            List<QualityCheckParams> existing,
            boolean requireAtLeastOne) {
        List<QualityCheckParams> existingParams = existing == null ? List.of() : existing;
        if (incoming == null) {
            incoming = List.of();
        }
        if (incoming.isEmpty() && requireAtLeastOne) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "At least one quality check parameter is required");
        }
        if (incoming.isEmpty() && existingParams.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, QualityCheckParams> existingById = indexParametersById(existingParams);
        Set<String> seenIds = new HashSet<>();
        List<QualityCheckParams> result = new ArrayList<>();
        int nextId = nextParameterId(existingParams);
        for (QualityCheckParams check : incoming) {
            String parameterId = check.getParameterId();
            if (parameterId != null && !parameterId.isBlank()) {
                parameterId = parameterId.trim();
                if (!seenIds.add(parameterId)) {
                    throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                            "Duplicate parameterId: " + parameterId);
                }
                QualityCheckParams existingParam = existingById.get(parameterId);
                if (existingParam == null) {
                    throw new AcemException(ErrorCode.INVALID_REQUEST,
                            "Unknown parameterId: " + parameterId);
                }
                assertExistingParameterUnchanged(existingParam, check, parameterId);
                SpecificationRangeDto specification = copySpecification(existingParam.getSpecification());
                if (isBlankUnit(specification) && !isBlankUnit(check.getSpecification())) {
                    specification = resolveSpecification(check.getSpecification());
                }
                result.add(QualityCheckParams.builder()
                        .parameterId(existingParam.getParameterId())
                        .parameterName(existingParam.getParameterName())
                        .specification(specification)
                        .noOfSamples(existingParam.getNoOfSamples())
                        .isActive(resolveNestedActive(check.getIsActive()))
                        .build());
                continue;
            }
            parameterId = String.valueOf(nextId++);
            if (!seenIds.add(parameterId)) {
                throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE,
                        "Duplicate parameterId: " + parameterId);
            }
            result.add(buildParameter(check, parameterId));
        }
        for (String existingId : existingById.keySet()) {
            if (!seenIds.contains(existingId)) {
                throw new AcemException(ErrorCode.READONLY_FIELD,
                        "Existing parameter cannot be removed: " + existingId);
            }
        }
        return result;
    }

    public static QualityCheckParams toParameterDto(QualityCheckParams parameter) {
        if (parameter == null) {
            return null;
        }
        return QualityCheckParams.builder()
                .parameterId(parameter.getParameterId())
                .parameterName(parameter.getParameterName())
                .specification(copySpecification(parameter.getSpecification()))
                .noOfSamples(parameter.getNoOfSamples())
                .isActive(resolveNestedActive(parameter.getIsActive()))
                .build();
    }

    private QualityCheckParams buildParameter(QualityCheckParams check, String parameterId) {
        SpecificationRangeDto range = resolveSpecification(check.getSpecification());
        if (range != null && range.getMinValue() != null && range.getMaxValue() != null
                && range.getMinValue() > range.getMaxValue()) {
            throw new AcemException(ErrorCode.INVALID_REQUEST,
                    "minValue cannot exceed maxValue for parameter " + parameterId);
        }
        Integer noOfSamples = check.getNoOfSamples();
        if (noOfSamples == null || noOfSamples < 1) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "No of samples is required");
        }
        return QualityCheckParams.builder()
                .parameterId(parameterId)
                .parameterName(MasterDataValidation.validateName(check.getParameterName()))
                .specification(range)
                .noOfSamples(noOfSamples)
                .isActive(resolveNestedActive(check.getIsActive()))
                .build();
    }

    private static Map<String, QualityCheckParams> indexParametersById(List<QualityCheckParams> parameters) {
        Map<String, QualityCheckParams> indexed = new HashMap<>();
        for (QualityCheckParams parameter : parameters) {
            if (parameter.getParameterId() == null || parameter.getParameterId().isBlank()) {
                continue;
            }
            indexed.put(parameter.getParameterId().trim(), parameter);
        }
        return indexed;
    }

    private static void assertExistingParameterUnchanged(
            QualityCheckParams existing,
            QualityCheckParams incoming,
            String parameterId) {
        String existingName = existing.getParameterName() == null ? "" : existing.getParameterName().trim();
        String incomingName = incoming.getParameterName() == null ? "" : incoming.getParameterName().trim();
        if (!existingName.equals(incomingName)) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing parameter name cannot be changed: " + parameterId);
        }
        if (!Objects.equals(existing.getNoOfSamples(), incoming.getNoOfSamples())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing parameter sample count cannot be changed: " + parameterId);
        }
        SpecificationRangeDto existingRange = existing.getSpecification();
        SpecificationRangeDto incomingRange = incoming.getSpecification();
        if (!Objects.equals(existingRange == null ? null : existingRange.getMinValue(),
                incomingRange == null ? null : incomingRange.getMinValue())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing parameter min value cannot be changed: " + parameterId);
        }
        if (!Objects.equals(existingRange == null ? null : existingRange.getMaxValue(),
                incomingRange == null ? null : incomingRange.getMaxValue())) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing parameter max value cannot be changed: " + parameterId);
        }
        if (!isBlankUnit(existingRange) && !unitEquals(existingRange, incomingRange)) {
            throw new AcemException(ErrorCode.READONLY_FIELD,
                    "Existing parameter unit cannot be changed: " + parameterId);
        }
    }

    private SpecificationRangeDto resolveSpecification(SpecificationRangeDto range) {
        if (range == null) {
            return null;
        }
        if (range.getUnitId() != null) {
            UnitMaster unit = unitMasterRepository.findFirstById(range.getUnitId()).orElse(null);
            if (unit != null) {
                return SpecificationRangeDto.builder()
                        .minValue(range.getMinValue())
                        .maxValue(range.getMaxValue())
                        .unitId(range.getUnitId())
                        .unit(resolveUnitDisplay(unit))
                        .build();
            }
            if (range.getUnit() != null && !range.getUnit().isBlank()) {
                return SpecificationRangeDto.builder()
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
            return SpecificationRangeDto.builder()
                    .minValue(range.getMinValue())
                    .maxValue(range.getMaxValue())
                    .unit(range.getUnit().trim())
                    .build();
        }
        return SpecificationRangeDto.builder()
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

    private static boolean isBlankUnit(SpecificationRangeDto range) {
        if (range == null) {
            return true;
        }
        if (range.getUnitId() != null) {
            return false;
        }
        return range.getUnit() == null || range.getUnit().isBlank();
    }

    private static boolean unitEquals(SpecificationRangeDto left, SpecificationRangeDto right) {
        String leftUnit = left == null || left.getUnit() == null ? "" : left.getUnit().trim();
        String rightUnit = right == null || right.getUnit() == null ? "" : right.getUnit().trim();
        return leftUnit.equalsIgnoreCase(rightUnit);
    }

    private static SpecificationRangeDto copySpecification(SpecificationRangeDto range) {
        if (range == null) {
            return null;
        }
        return SpecificationRangeDto.builder()
                .minValue(range.getMinValue())
                .maxValue(range.getMaxValue())
                .unitId(range.getUnitId())
                .unit(range.getUnit())
                .build();
    }

    private static int nextParameterId(List<QualityCheckParams> parameters) {
        int max = 0;
        for (QualityCheckParams parameter : parameters) {
            if (parameter.getParameterId() == null || parameter.getParameterId().isBlank()) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(parameter.getParameterId().trim()));
            } catch (NumberFormatException ignored) {
                max++;
            }
        }
        return max + 1;
    }

    private static boolean resolveNestedActive(Boolean isActive) {
        return isActive == null || Boolean.TRUE.equals(isActive);
    }
}
