package in.co.bel.acempp.dto.statictestingfacility;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.BemStaticTestingDetails;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.MainMotorStaticTestingDetails;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.StaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deserializes QC / shared {@link StaticMotorData} motors, resolving polymorphic
 * {@link StaticTestingDetails} from motor {@code subType} (MAIN_MOTOR vs BEM).
 *
 * <p>Uses {@link DeserializationContext} / {@link ObjectCodec} so it works both when the
 * top-level codec is an {@code ObjectMapper} (manufacturing) and when nested under an
 * {@code ObjectReader} (QC division {@code lenientConvert}).
 */
public class StaticMotorDataListDeserializer extends JsonDeserializer<List<StaticMotorData>> {

    @Override
    public List<StaticMotorData> deserialize(
            JsonParser parser,
            DeserializationContext context) throws IOException {
        JsonNode root = context.readTree(parser);
        if (root == null || root.isNull()) {
            return null;
        }
        if (!root.isArray()) {
            throw JsonMappingException.from(parser, "motors must be an array");
        }

        List<StaticMotorData> motors = new ArrayList<>();
        for (JsonNode node : root) {
            if (node == null || node.isNull()) {
                continue;
            }
            if (!node.isObject()) {
                throw JsonMappingException.from(parser, "Each STF motor must be an object");
            }
            motors.add(readMotor(parser, context, node));
        }
        return motors;
    }

    private StaticMotorData readMotor(
            JsonParser parser,
            DeserializationContext context,
            JsonNode node) throws IOException {
        StfMotorSubType subType = readEnumOptional(parser, node, "subType", StfMotorSubType.class);
        StaticTestingDetails details = readDetails(parser, context, node.get("staticTestingDetails"), subType);

        return StaticMotorData.builder()
                .motorId(readTextOptional(node, "motorId"))
                .subType(subType)
                .stfTestNo(readTextOptional(node, "stfTestNo"))
                .bemNo(readTextOptional(node, "bemNo"))
                .motorSubmissionType(readEnumOptional(parser, node, "motorSubmissionType", Status.class))
                .motorSubmissionStatus(readEnumOptional(parser, node, "motorSubmissionStatus", Status.class))
                .staticTestingDetails(details)
                .build();
    }

    private StaticTestingDetails readDetails(
            JsonParser parser,
            DeserializationContext context,
            JsonNode node,
            StfMotorSubType subType) throws IOException {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw JsonMappingException.from(parser, "staticTestingDetails must be an object or null");
        }
        Class<? extends StaticTestingDetails> type = resolveDetailsType(subType, node);
        ObjectCodec codec = parser.getCodec();
        JsonParser detailsParser = node.traverse(codec);
        if (detailsParser.getCurrentToken() == null) {
            detailsParser.nextToken();
        }
        return context.readValue(detailsParser, type);
    }

    private Class<? extends StaticTestingDetails> resolveDetailsType(
            StfMotorSubType subType,
            JsonNode detailsNode) {
        if (subType == StfMotorSubType.MAIN_MOTOR) {
            return MainMotorStaticTestingDetails.class;
        }
        if (subType == StfMotorSubType.BEM) {
            return BemStaticTestingDetails.class;
        }
        // Infer when subType is missing (legacy / partial payloads).
        if (detailsNode.has("bemData")
                || detailsNode.has("igniterDetails")
                || hasBemTestHeaderMarkers(detailsNode.get("testHeader"))) {
            return BemStaticTestingDetails.class;
        }
        return MainMotorStaticTestingDetails.class;
    }

    private boolean hasBemTestHeaderMarkers(JsonNode testHeader) {
        if (testHeader == null || !testHeader.isObject()) {
            return false;
        }
        return testHeader.has("loadingNo")
                || testHeader.has("wtOfGrain")
                || testHeader.has("grainConfiguration")
                || testHeader.has("firingNo");
    }

    private String readTextOptional(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private <E extends Enum<E>> E readEnumOptional(
            JsonParser parser,
            JsonNode node,
            String field,
            Class<E> enumType) throws JsonMappingException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw JsonMappingException.from(parser, "Invalid " + field + ": " + text);
        }
    }
}
