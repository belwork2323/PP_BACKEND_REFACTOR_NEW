package in.co.bel.acempp.dto.postcureoperations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class PostCureMotorListDeserializer
        extends JsonDeserializer<List<PostCureMotor>> {

    @Override
    public List<PostCureMotor> deserialize(
            JsonParser parser,
            DeserializationContext context) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);
        if (!(codec instanceof ObjectMapper mapper)) {
            throw JsonMappingException.from(parser,
                    "Post Cure motors require an ObjectMapper codec");
        }
        JavaType type = mapper.getTypeFactory().constructCollectionType(
                List.class, PostCureMotor.class);
        return mapper.readerFor(type)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(node);
    }
}
