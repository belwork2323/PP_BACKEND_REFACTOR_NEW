package in.co.bel.acempp.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TempUploadResponseDTO {
    private String fileId;
    private String originalFileName;
    private String tempFilePath;
    private String mimeType;
    private long fileSize;
    private String status; // TEMP_SUCCESS, TEMP_FAILED
    private String errorMessage;
}
