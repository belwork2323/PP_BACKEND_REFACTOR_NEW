package in.co.bel.acempp.dto.batch;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Accepts either a mixer code string ({@code "MX-9"}) or a master-data object
 * ({@code {"code":"MX-9","name":"..."}}) for identification-sheet {@code mixerType}.
 */
public class MixerTypeDeserializer extends JsonDeserializer<MasterDataDto> {

    @Override
    public MasterDataDto deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token == JsonToken.VALUE_STRING) {
            String code = parser.getText() != null ? parser.getText().trim() : "";
            if (code.isEmpty()) {
                return null;
            }
            return MasterDataDto.builder().code(code).build();
        }
        if (token == JsonToken.START_OBJECT) {
            JsonNode node = parser.getCodec().readTree(parser);
            if (node == null || node.isNull()) {
                return null;
            }
            String code = textOrNull(node.get("code"));
            if (code == null || code.isBlank()) {
                code = textOrNull(node.get("mixerType"));
            }
            if (code == null || code.isBlank()) {
                return null;
            }
            Long id = null;
            JsonNode idNode = node.get("id");
            if (idNode != null && idNode.isNumber()) {
                id = idNode.longValue();
            }
            return MasterDataDto.builder()
                    .id(id)
                    .code(code.trim())
                    .name(textOrNull(node.get("name")))
                    .build();
        }
        return (MasterDataDto) context.handleUnexpectedToken(MasterDataDto.class, parser);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value != null ? value.trim() : null;
    }
}
