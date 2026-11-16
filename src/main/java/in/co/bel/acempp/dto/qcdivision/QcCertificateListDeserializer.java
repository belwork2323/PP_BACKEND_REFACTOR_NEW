package in.co.bel.acempp.dto.qcdivision;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accepts qcCertificate as either string fileIds or DocumentDto objects.
 */
public class QcCertificateListDeserializer extends JsonDeserializer<List<DocumentDto>> {

    @Override
    public List<DocumentDto> deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw JsonMappingException.from(parser, "qcCertificate must be an array");
        }

        List<DocumentDto> certificates = new ArrayList<>();
        for (JsonNode element : node) {
            if (element == null || element.isNull()) {
                continue;
            }
            if (element.isTextual()) {
                String fileId = element.asText().trim();
                if (!fileId.isEmpty()) {
                    certificates.add(DocumentDto.builder().fileId(fileId).build());
                }
                continue;
            }
            if (element.isObject()) {
                DocumentDto doc = parser.getCodec().treeToValue(element, DocumentDto.class);
                if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
                    certificates.add(doc);
                }
                continue;
            }
            throw JsonMappingException.from(
                    parser,
                    "qcCertificate entries must be fileId strings or document objects");
        }
        return certificates;
    }
}
