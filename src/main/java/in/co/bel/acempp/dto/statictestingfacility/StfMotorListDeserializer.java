package in.co.bel.acempp.dto.statictestingfacility;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.BemStaticTestingDetails;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.MainMotorStaticTestingDetails;
import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.StaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StfMotorListDeserializer extends JsonDeserializer<List<StfMotorRequest>> {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "motorId", "subType", "stfTestNo", "motorSubmissionType", "staticTestingDetails");

    @Override
    public List<StfMotorRequest> deserialize(
            JsonParser parser,
            DeserializationContext context) throws IOException {
        if (!(parser.getCodec() instanceof ObjectMapper mapper)) {
            throw JsonMappingException.from(parser, "STF motors require an ObjectMapper codec");
        }
        JsonNode root = mapper.readTree(parser);
        if (!root.isArray()) {
            throw JsonMappingException.from(parser, "motors must be an array");
        }

        List<StfMotorRequest> motors = new ArrayList<>();
        for (JsonNode node : root) {
            if (!node.isObject()) {
                throw JsonMappingException.from(parser, "Each STF motor must be an object");
            }
            rejectUnknownFields(parser, node);
            StfMotorSubType subType = readEnum(
                    parser, node, "subType", StfMotorSubType.class);
            StaticTestingDetails details = readDetails(parser, mapper, node.get("staticTestingDetails"), subType);
            motors.add(StfMotorRequest.builder()
                    .motorId(readText(parser, node, "motorId"))
                    .subType(subType)
                    .stfTestNo(readText(parser, node, "stfTestNo"))
                    .motorSubmissionType(readEnum(
                            parser, node, "motorSubmissionType", Status.class))
                    .staticTestingDetails(details)
                    .build());
        }
        return motors;
    }

    private StaticTestingDetails readDetails(
            JsonParser parser,
            ObjectMapper mapper,
            JsonNode node,
            StfMotorSubType subType) throws IOException {
        if (node == null || node.isNull()) return null;
        if (!node.isObject()) {
            throw JsonMappingException.from(parser, "staticTestingDetails must be an object or null");
        }
        Class<? extends StaticTestingDetails> type = subType == StfMotorSubType.MAIN_MOTOR
                ? MainMotorStaticTestingDetails.class
                : BemStaticTestingDetails.class;
        return mapper.readerFor(type)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(node);
    }

    private void rejectUnknownFields(JsonParser parser, JsonNode node) throws JsonMappingException {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!ALLOWED_FIELDS.contains(name)) {
                throw JsonMappingException.from(parser, "Unknown STF motor field: " + name);
            }
        }
    }

    private String readText(JsonParser parser, JsonNode node, String field) throws JsonMappingException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw JsonMappingException.from(parser, field + " must be a non-blank string");
        }
        return value.textValue();
    }

    private <E extends Enum<E>> E readEnum(
            JsonParser parser,
            JsonNode node,
            String field,
            Class<E> enumType) throws JsonMappingException {
        String value = readText(parser, node, field);
        try {
            return Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw JsonMappingException.from(parser, "Invalid " + field + ": " + value);
        }
    }
}
