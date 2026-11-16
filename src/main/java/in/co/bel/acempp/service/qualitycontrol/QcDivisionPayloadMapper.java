package in.co.bel.acempp.service.qualitycontrol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.DivisionDataResponse;
import in.co.bel.acempp.dto.qcdivision.QcDivisionPayloads.DivisionDetailInput;
import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import in.co.bel.acempp.util.DateTimeConversionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class QcDivisionPayloadMapper {

    private static final Set<String> AUDIT_DATE_TIME_FIELDS = Set.of(
            "submittedAt", "approvedAt", "actionAt");

    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> toMaps(List<DivisionDetailInput> details) {
        if (details == null) return null;
        return details.stream().map(this::toMap).toList();
    }

    public Map<String, Object> toMap(DivisionDetailInput detail) {
        if (detail == null) {
            throw new AcemException(ErrorCode.INVALID_REQUEST, "QC division detail cannot be null");
        }
        ObjectNode node = objectMapper.createObjectNode();
        putText(node, "division", detail.getDivision());
        putText(node, "subType", detail.getSubType());
        if (detail.getDivisionSubmissionType() != null) {
            node.put("divisionSubmissionType", detail.getDivisionSubmissionType().name());
        }
        if (detail.getData() != null) {
            node.set("data", removeNulls(objectMapper.valueToTree(detail.getData())));
        }
        normalizePostCureInhibitorType(node, detail);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.convertValue(node, LinkedHashMap.class);
        return result;
    }

    public DivisionDataResponse toResponse(
            String division,
            String subType,
            Map<String, Object> data) {
        if (data == null) return null;
        Class<? extends DivisionDataResponse> type = responseType(effectiveDivision(division, subType));
        JsonNode normalized = objectMapper.valueToTree(normalizeAuditDateTimes(data));
        try {
            return objectMapper.readerFor(type)
                    .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(normalized);
        } catch (java.io.IOException exception) {
            throw new AcemException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid QC division response data for " + division + ": "
                            + exception.getMessage());
        }
    }

    public Map<String, Object> responseToMap(DivisionDataResponse data) {
        if (data == null) return Collections.emptyMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.convertValue(data, LinkedHashMap.class);
        return result;
    }

    private Class<? extends DivisionDataResponse> responseType(String division) {
        return switch (division) {
            case "RAW_MATERIAL_REVALIDATION" -> QcDivisionPayloads.RawMaterialRevalidationData.class;
            case "RAW_MATERIAL_PROCESSING" -> QcDivisionPayloads.RawMaterialProcessingDataResponse.class;
            case "MIXING" -> QcDivisionPayloads.MixingDataResponse.class;
            case "HARDWARE" -> QcDivisionPayloads.HardwareDataResponse.class;
            case "CASTING" -> QcDivisionPayloads.CastingDataResponse.class;
            case "CURING" -> QcDivisionPayloads.CuringDataResponse.class;
            case "DE_CORING" -> QcDivisionPayloads.DeCoringDataResponse.class;
            case "TRIMMING" -> QcDivisionPayloads.TrimmingDataResponse.class;
            case "POST_CURE" -> QcDivisionPayloads.PostCureDataResponse.class;
            case "NDT" -> QcDivisionPayloads.NdtDataResponse.class;
            case "QC", "PROPELLANT_PROPERTIES" -> QcDivisionPayloads.PropellantDataResponse.class;
            case "WEIGHTMENT" -> QcDivisionPayloads.WeightmentDataResponse.class;
            case "STATIC_TEST_FACILITY" -> QcDivisionPayloads.StaticTestFacilityDataResponse.class;
            default -> throw new AcemException(
                    ErrorCode.INVALID_REQUEST, "Unsupported QC division response: " + division);
        };
    }

    private String effectiveDivision(String division, String subType) {
        String normalizedDivision = normalize(division);
        if ("RAW_MATERIAL".equals(normalizedDivision)) return normalize(subType);
        if ("POST_CURE_OPERATION".equals(normalizedDivision)) return "POST_CURE";
        return normalizedDivision;
    }

    private String normalize(String value) {
        return value == null ? null
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private void putText(ObjectNode node, String field, String value) {
        if (value != null) node.put(field, value);
    }

    private JsonNode removeNulls(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> nullFields = new ArrayList<>();
            objectNode.properties().forEach(entry -> {
                if (entry.getValue().isNull()) nullFields.add(entry.getKey());
                else removeNulls(entry.getValue());
            });
            nullFields.forEach(objectNode::remove);
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::removeNulls);
        }
        return node;
    }

    private void normalizePostCureInhibitorType(
            ObjectNode divisionNode,
            DivisionDetailInput detail) {
        if (!"POST_CURE".equals(effectiveDivision(detail.getDivision(), detail.getSubType()))) {
            return;
        }
        JsonNode motors = divisionNode.path("data").path("postCureMotorDetails");
        if (!(motors instanceof ArrayNode motorArray)) return;

        motorArray.forEach(motorNode -> {
            if (!(motorNode instanceof ObjectNode motor)) return;
            JsonNode inhibitionNode = motor.get("inhibitionDetails");
            if (!(inhibitionNode instanceof ObjectNode inhibition)) return;
            // Keep nested inhibitorType and lift a copy to motor-level when missing.
            // Previously remove() stripped nested type and left FE unable to reopen Inhibition.
            JsonNode nestedType = inhibition.get("inhibitorType");
            if (!motor.hasNonNull("inhibitorType") && nestedType != null && !nestedType.isNull()) {
                motor.set("inhibitorType", nestedType.deepCopy());
            }
            if ((!inhibition.hasNonNull("inhibitorType") || inhibition.get("inhibitorType").isNull())
                    && motor.hasNonNull("inhibitorType")) {
                inhibition.set("inhibitorType", motor.get("inhibitorType").deepCopy());
            }
        });
    }

    private Map<String, Object> normalizeAuditDateTimes(Map<String, Object> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> normalized.put(key, normalizeValue(key, value)));
        return normalized;
    }

    private Object normalizeValue(String field, Object value) {
        if (field != null && AUDIT_DATE_TIME_FIELDS.contains(field)) {
            LocalDateTime converted = DateTimeConversionUtil.toLocalDateTime(value);
            if (value != null && converted == null) {
                throw new AcemException(
                        ErrorCode.INVALID_REQUEST,
                        "Invalid QC audit timestamp for " + field + ": " + value);
            }
            return converted;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> normalized.put(
                    String.valueOf(key), normalizeValue(String.valueOf(key), nestedValue)));
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(item -> normalizeValue(null, item))
                    .toList();
        }
        return value;
    }
}
