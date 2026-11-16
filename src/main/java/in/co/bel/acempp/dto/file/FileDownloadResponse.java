package in.co.bel.acempp.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadResponse {
    private String fileId;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private String base64Content; // Raw file data as Base64 string
}