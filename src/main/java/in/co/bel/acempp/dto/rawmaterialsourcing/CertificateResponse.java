package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CertificateResponse {
    private String fileId;
    private String fileName;
    private String fileUrl;
    private String certificateType;
    private String status;
    private String mimeType;
    private Boolean readOnly;
}