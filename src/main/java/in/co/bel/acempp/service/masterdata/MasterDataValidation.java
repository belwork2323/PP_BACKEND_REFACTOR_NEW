package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.masterdata.MasterDataFieldDef;
import in.co.bel.acempp.dto.masterdata.MasterDataRecordDto;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;
import in.co.bel.acempp.entity.batch.EquipmentTypeMaster;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.repository.EquipmentTypeMasterRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MasterDataValidation {

    private MasterDataValidation() {
    }

    public static boolean isActiveFlag(Boolean isActive) {
        return isActive == null || Boolean.TRUE.equals(isActive);
    }

    public static String requireTrimmed(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, fieldLabel + " is required");
        }
        return value.trim();
    }

    public static String validateCode(String raw) {
        String code = requireTrimmed(raw, "Code");
        if (code.length() > 64) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Code must not exceed 64 characters");
        }
        if (!Pattern.matches(MasterDataCatalog.CODE_PATTERN, code)) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR,
                    "Code must start with a letter or digit and contain only letters, digits, . _ / \\ -");
        }
        return code;
    }

    public static String validateName(String raw) {
        String name = requireTrimmed(raw, "Name");
        if (name.length() > 255) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Name must not exceed 255 characters");
        }
        if (!Pattern.matches(MasterDataCatalog.MASTER_DATA_NAME_PATTERN, name)) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR,
                    "Name must start with a letter or digit and contain only letters, digits, spaces, hyphen, underscore, / and \\");
        }
        return name;
    }

    public static String validateInsulationType(String raw) {
        String insulationType = requireTrimmed(raw, "Insulation type");
        if (insulationType.length() > 64) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR, "Insulation type must not exceed 64 characters");
        }
        if (!Pattern.matches(MasterDataCatalog.MASTER_DATA_NAME_PATTERN, insulationType)) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR,
                    "Insulation type must start with a letter or digit and contain only letters, digits, spaces, hyphen, underscore, / and \\");
        }
        return insulationType;
    }

    public static void assertUniqueName(String name, Long excludeId, List<MasterDataRecordDto> existing) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        boolean duplicate = (existing == null ? List.<MasterDataRecordDto>of() : existing).stream()
                .filter(record -> excludeId == null || !Objects.equals(record.getId(), excludeId))
                .anyMatch(record -> record.getName() != null
                        && record.getName().trim().toLowerCase(Locale.ROOT).equals(normalized));
        if (duplicate) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_NAME, "Name already exists: " + name);
        }
    }

    public static void assertUniqueTypeCode(String typeCode, Long excludeId, List<MasterDataRecordDto> existing) {
        String normalized = typeCode.trim().toUpperCase(Locale.ROOT);
        boolean duplicate = (existing == null ? List.<MasterDataRecordDto>of() : existing).stream()
                .filter(record -> excludeId == null || !Objects.equals(record.getId(), excludeId))
                .anyMatch(record -> {
                    if (record.getAttributes() == null) {
                        return false;
                    }
                    Object raw = record.getAttributes().get("typeCode");
                    return raw != null
                            && raw.toString().trim().toUpperCase(Locale.ROOT).equals(normalized);
                });
        if (duplicate) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Type code already exists: " + typeCode);
        }
    }

    public static String validateEquipmentType(String raw, EquipmentTypeMasterRepository repository) {
        String typeCode = validateCode(raw);
        String normalized = typeCode.toUpperCase(Locale.ROOT);

        List<EquipmentTypeMaster> all = repository.findAll();
        EquipmentTypeMaster type = repository.findByTypeCode(typeCode)
                .or(() -> repository.findByTypeCode(normalized))
                .or(() -> all.stream()
                        .filter(item -> equalsIgnoreCaseTrimmed(item.getTypeCode(), normalized))
                        .findFirst())
                .or(() -> all.stream()
                        .filter(item -> equalsIgnoreCaseTrimmed(item.getCode(), normalized))
                        .findFirst())
                .or(() -> all.stream()
                        .filter(item -> normalizeToTypeCode(item.getName()).equals(normalized))
                        .findFirst())
                .or(() -> all.stream()
                        .filter(item -> equalsIgnoreCaseTrimmed(item.getName(), typeCode))
                        .findFirst())
                .orElseThrow(() -> new AcemException(ErrorCode.VALIDATION_ERROR,
                        "Unknown equipment type: " + typeCode
                                + ". Create it under NDT Equipment Types Master (type code) first."));

        if (type.getIsActive() != null && !Boolean.TRUE.equals(type.getIsActive())) {
            throw new AcemException(ErrorCode.VALIDATION_ERROR,
                    "Equipment type is inactive: " + typeCode);
        }
        if (type.getTypeCode() != null && !type.getTypeCode().isBlank()) {
            return type.getTypeCode().trim();
        }
        return normalized;
    }

    /** "Mag NDT" → "MAG_NDT" for matching equipment type attributes. */
    public static String normalizeToTypeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static boolean equalsIgnoreCaseTrimmed(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    public static void assertUniqueUnitCode(String unitCode, Long excludeId, List<MasterDataRecordDto> existing) {
        String normalized = unitCode.trim().toUpperCase(Locale.ROOT);
        boolean duplicate = (existing == null ? List.<MasterDataRecordDto>of() : existing).stream()
                .filter(record -> excludeId == null || !Objects.equals(record.getId(), excludeId))
                .anyMatch(record -> {
                    if (record.getAttributes() == null) {
                        return false;
                    }
                    Object raw = record.getAttributes().get("unitCode");
                    return raw != null
                            && raw.toString().trim().toUpperCase(Locale.ROOT).equals(normalized);
                });
        if (duplicate) {
            throw new AcemException(ErrorCode.DUPLICATE_MASTER_CODE, "Unit code already exists: " + unitCode);
        }
    }

    public static Map<String, Object> validateAttributes(
            MasterDataTypeDescriptor schema,
            Map<String, Object> attributes) {
        Map<String, Object> incoming = attributes == null ? Map.of() : attributes;
        Set<String> allowed = schema.getFields().stream()
                .filter(f -> Boolean.TRUE.equals(f.getAttribute()))
                .map(MasterDataFieldDef::getKey)
                .collect(Collectors.toSet());

        for (String key : incoming.keySet()) {
            if (!allowed.contains(key)) {
                throw new AcemException(ErrorCode.VALIDATION_ERROR, "Unknown attribute: " + key);
            }
        }

        Map<String, Object> validated = new LinkedHashMap<>();
        for (MasterDataFieldDef field : schema.getFields()) {
            if (!Boolean.TRUE.equals(field.getAttribute())) {
                continue;
            }
            Object raw = incoming.get(field.getKey());
            if (raw == null || (raw instanceof String s && s.trim().isEmpty())) {
                if (Boolean.TRUE.equals(field.getRequired())) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR, field.getLabel() + " is required");
                }
                continue;
            }
            validated.put(field.getKey(), coerceAndValidate(field, raw));
        }
        return validated;
    }

    private static Object coerceAndValidate(MasterDataFieldDef field, Object raw) {
        String dataType = field.getDataType() == null ? "STRING" : field.getDataType().toUpperCase(Locale.ROOT);
        return switch (dataType) {
            case "INTEGER" -> {
                if (!(raw instanceof Number) || raw instanceof Double || raw instanceof Float) {
                    if (raw instanceof String) {
                        throw new AcemException(ErrorCode.VALIDATION_ERROR,
                                field.getLabel() + " must be an integer");
                    }
                }
                int value;
                if (raw instanceof Number number) {
                    if (raw instanceof Double || raw instanceof Float
                            || (raw instanceof java.math.BigDecimal bd && bd.scale() > 0)) {
                        throw new AcemException(ErrorCode.VALIDATION_ERROR,
                                field.getLabel() + " must be an integer");
                    }
                    value = number.intValue();
                    if (number.doubleValue() != value) {
                        throw new AcemException(ErrorCode.VALIDATION_ERROR,
                                field.getLabel() + " must be an integer");
                    }
                } else {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " must be an integer");
                }
                if (field.getMin() != null && value < field.getMin()) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " must be at least " + field.getMin());
                }
                if (field.getMax() != null && value > field.getMax()) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " must be at most " + field.getMax());
                }
                yield value;
            }
            case "STRING" -> {
                if (!(raw instanceof String)) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " must be a string");
                }
                String value = ((String) raw).trim();
                if (field.getMaxLength() != null && value.length() > field.getMaxLength()) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " must not exceed " + field.getMaxLength() + " characters");
                }
                if (field.getPattern() != null && !Pattern.matches(field.getPattern(), value)) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " has an invalid format");
                }
                yield value;
            }
            case "BOOLEAN" -> {
                if (!(raw instanceof Boolean)) {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " must be a boolean");
                }
                yield raw;
            }
            case "NUMBER", "DOUBLE" -> {
                double value;
                if (raw instanceof Number number) {
                    value = number.doubleValue();
                } else if (raw instanceof String s) {
                    try {
                        value = Double.parseDouble(s.trim());
                    } catch (NumberFormatException ex) {
                        throw new AcemException(ErrorCode.VALIDATION_ERROR,
                                field.getLabel() + " must be a number");
                    }
                } else {
                    throw new AcemException(ErrorCode.VALIDATION_ERROR,
                            field.getLabel() + " must be a number");
                }
                yield value;
            }
            default -> throw new AcemException(ErrorCode.VALIDATION_ERROR,
                    "Unsupported field type for " + field.getKey());
        };
    }

    public static MasterDataRecordDto record(Long id, String code, String name, Boolean isActive,
                                             Map<String, Object> attributes) {
        return record(id, code, name, isActive, attributes, null);
    }

    public static PersonInfo auditCreatedBy(MasterDataAuditable auditable) {
        return auditable == null ? null : auditable.getCreatedBy();
    }

    public static LocalDateTime auditCreatedOn(MasterDataAuditable auditable) {
        return auditable == null ? null : auditable.getCreatedOn();
    }

    public static PersonInfo auditUpdatedBy(MasterDataAuditable auditable) {
        return auditable == null ? null : auditable.getUpdatedBy();
    }

    public static LocalDateTime auditUpdatedOn(MasterDataAuditable auditable) {
        return auditable == null ? null : auditable.getUpdatedOn();
    }

    public static MasterDataRecordDto record(Long id, String code, String name, Boolean isActive,
                                             Map<String, Object> attributes, MasterDataAuditable auditable) {
        return MasterDataRecordDto.builder()
                .id(id)
                .code(code)
                .name(name)
                .isActive(isActiveFlag(isActive))
                .attributes(attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes))
                .createdBy(auditable == null ? null : auditable.getCreatedBy())
                .createdOn(auditable == null ? null : auditable.getCreatedOn())
                .updatedBy(auditable == null ? null : auditable.getUpdatedBy())
                .updatedOn(auditable == null ? null : auditable.getUpdatedOn())
                .build();
    }

    public static boolean matchesSearch(MasterDataRecordDto record, String search) {
        if (search == null || search.trim().isEmpty()) {
            return true;
        }
        String q = search.trim().toLowerCase(Locale.ROOT);
        return (record.getCode() != null && record.getCode().toLowerCase(Locale.ROOT).contains(q))
                || (record.getName() != null && record.getName().toLowerCase(Locale.ROOT).contains(q));
    }

    public static boolean matchesActiveFilter(MasterDataRecordDto record, Boolean isActiveFilter) {
        if (isActiveFilter == null) {
            return true;
        }
        return Objects.equals(record.getIsActive(), isActiveFilter);
    }
}
