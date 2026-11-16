package in.co.bel.acempp.dto.rawmaterialsourcing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.co.bel.acempp.dto.rocketmotorcasing.DocumentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RMS certificate payload. Intentionally does <strong>not</strong> extend
 * {@link DocumentDto} so Jakarta {@code @NotBlank} on {@code fileId} does not
 * short-circuit service-tier validation (fileId is required on SUBMIT in
 * {@code RawMaterialSourcingService}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateRequest {
    private String fileId;
    private String status;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Boolean readOnly;
    private String certificateType;

    /** Copy into a {@link DocumentDto} for shared file-metadata enrichment. */
    public DocumentDto toDocumentDto() {
        return DocumentDto.builder()
                .fileId(fileId)
                .status(status)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .mimeType(mimeType)
                .readOnly(readOnly)
                .build();
    }

    public static CertificateRequest fromDocumentDto(DocumentDto doc, String certificateType) {
        if (doc == null) {
            return CertificateRequest.builder().certificateType(certificateType).build();
        }
        return CertificateRequest.builder()
                .fileId(doc.getFileId())
                .status(doc.getStatus())
                .fileName(doc.getFileName())
                .fileUrl(doc.getFileUrl())
                .mimeType(doc.getMimeType())
                .readOnly(doc.getReadOnly())
                .certificateType(certificateType)
                .build();
    }
}
