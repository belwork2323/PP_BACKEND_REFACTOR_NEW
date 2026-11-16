package in.co.bel.acempp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FileIdExtractionUtil {

    private FileIdExtractionUtil() {}

    public static Set<String> extractFromDocuments(List<? extends DocumentDto> docs) {
        Set<String> fileIds = new HashSet<>();
        if (docs == null) {
            return fileIds;
        }
        for (DocumentDto doc : docs) {
            extractFromDocument(doc, fileIds);
        }
        return fileIds;
    }

    public static void extractFromDocument(DocumentDto doc, Set<String> targetSet) {
        if (doc != null && doc.getFileId() != null && !doc.getFileId().isBlank()) {
            targetSet.add(doc.getFileId().trim());
        }
    }

    public static Set<String> extractFromJsonNode(JsonNode node) {
        Set<String> fileIds = new HashSet<>();
        collectFileIdsFromNode(node, fileIds);
        return fileIds;
    }

    public static void collectFileIdsFromNode(JsonNode node, Set<String> targetSet) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            if (objectNode.hasNonNull("fileId")) {
                String fileId = objectNode.get("fileId").asText().trim();
                if (!fileId.isEmpty()) {
                    targetSet.add(fileId);
                }
            }
            objectNode.elements().forEachRemaining(child -> collectFileIdsFromNode(child, targetSet));
        } else if (node.isArray()) {
            node.forEach(element -> collectFileIdsFromNode(element, targetSet));
        }
    }

    public static Set<String> union(Collection<String>... sets) {
        Set<String> result = new HashSet<>();
        if (sets == null) {
            return result;
        }
        for (Collection<String> set : sets) {
            if (set != null) {
                result.addAll(set);
            }
        }
        return result;
    }
}
