package in.co.bel.acempp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments.MediaFile;
import in.co.bel.acempp.entity.FileAttachments;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileMetadataEnrichmentService {

    private final FileAttachmentLookupService fileAttachmentLookupService;

    public void enrichDocument(DocumentDto doc, Long subDepartmentId) {
        enrichDocument(doc, subDepartmentId, null);
    }

    public void enrichDocument(DocumentDto doc, Long subDepartmentId, String motorId) {
        if (doc == null || doc.getFileId() == null || doc.getFileId().isBlank()) {
            return;
        }
        resolveAttachment(subDepartmentId, doc.getFileId(), motorId).ifPresent(file -> applyToDocument(doc, file));
    }

    public void enrichDocuments(List<? extends DocumentDto> docs, Long subDepartmentId) {
        enrichDocuments(docs, subDepartmentId, null);
    }

    public void enrichDocuments(List<? extends DocumentDto> docs, Long subDepartmentId, String motorId) {
        if (docs == null) {
            return;
        }
        for (DocumentDto doc : docs) {
            enrichDocument(doc, subDepartmentId, motorId);
        }
    }


    public void enrichMediaFile(MediaFile media, Long subDepartmentId, String motorId) {
        if (media == null || media.getFileId() == null || media.getFileId().isBlank()) {
            return;
        }
        resolveAttachment(subDepartmentId, media.getFileId(), motorId).ifPresent(file -> {
            if (media.getFileName() == null || media.getFileName().isBlank()) {
                media.setFileName(file.getOriginalFileName());
            }
            if (media.getFileUrl() == null || media.getFileUrl().isBlank()) {
                media.setFileUrl(file.getFilePath());
            }
            if (media.getStatus() == null || media.getStatus().isBlank()) {
                media.setStatus(file.getStatus());
            }
            media.setReadOnly(file.isReadOnly());
        });
    }


    public void enrichJsonFileNodes(JsonNode node, Long subDepartmentId, String motorId) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            String nodeMotorId = motorId;
            if (objectNode.hasNonNull("motorId")) {
                String parsed = objectNode.get("motorId").asText().trim();
                if (!parsed.isEmpty()) {
                    nodeMotorId = parsed;
                }
            } else if (objectNode.hasNonNull("motorIdNo")) {
                String parsed = objectNode.get("motorIdNo").asText().trim();
                if (!parsed.isEmpty()) {
                    nodeMotorId = parsed;
                }
            }

            if (objectNode.hasNonNull("fileId")) {
                String fileId = objectNode.get("fileId").asText().trim();
                if (!fileId.isEmpty()) {
                    resolveAttachment(subDepartmentId, fileId, nodeMotorId).ifPresent(file -> {
                        if (!objectNode.hasNonNull("fileName") || objectNode.get("fileName").asText().isBlank()) {
                            objectNode.put("fileName", file.getOriginalFileName());
                        }
                        if (!objectNode.hasNonNull("fileUrl") || objectNode.get("fileUrl").asText().isBlank()) {
                            objectNode.put("fileUrl", file.getFilePath());
                        }
                        if (!objectNode.hasNonNull("mimeType") || objectNode.get("mimeType").asText().isBlank()) {
                            objectNode.put("mimeType", file.getMimeType());
                        }
                        if (!objectNode.hasNonNull("status") || objectNode.get("status").asText().isBlank()) {
                            objectNode.put("status", file.getStatus());
                        }
                        objectNode.put("readOnly", file.isReadOnly());
                    });
                }
            }
            String childMotorId = nodeMotorId;
            objectNode.elements().forEachRemaining(child -> enrichJsonFileNodes(child, subDepartmentId, childMotorId));
        } else if (node.isArray()) {
            node.forEach(element -> enrichJsonFileNodes(element, subDepartmentId, motorId));
        }
    }

    private void applyToDocument(DocumentDto doc, FileAttachments file) {
        if (doc.getFileName() == null || doc.getFileName().isBlank()) {
            doc.setFileName(file.getOriginalFileName());
        }
        if (doc.getFileUrl() == null || doc.getFileUrl().isBlank()) {
            doc.setFileUrl(file.getFilePath());
        }
        if (doc.getStatus() == null || doc.getStatus().isBlank()) {
            doc.setStatus(file.getStatus());
        }
        if (doc.getMimeType() == null || doc.getMimeType().isBlank()) {
            doc.setMimeType(file.getMimeType());
        }
        doc.setReadOnly(file.isReadOnly());
    }

    private Optional<FileAttachments> resolveAttachment(Long subDepartmentId, String fileId, String motorId) {
        return fileAttachmentLookupService.resolve(fileId, subDepartmentId, motorId);
    }
}
