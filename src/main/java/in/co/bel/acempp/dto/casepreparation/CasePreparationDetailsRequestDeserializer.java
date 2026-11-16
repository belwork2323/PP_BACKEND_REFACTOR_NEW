package in.co.bel.acempp.dto.casepreparation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class CasePreparationDetailsRequestDeserializer
        extends JsonDeserializer<CasePreparationDetailsRequest> {

    @Override
    public CasePreparationDetailsRequest deserialize(
            JsonParser parser,
            DeserializationContext context) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);
        if (!(codec instanceof ObjectMapper mapper)) {
            throw JsonMappingException.from(parser,
                    "Case Preparation details require an ObjectMapper codec");
        }
        return mapper.readerFor(CasePreparationDetailsRequest.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(node);
    }
}
