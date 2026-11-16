package in.co.bel.acempp.dto.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStatusResponse {
    private String fileId;
    private String status;
    private String originalFileName; // Optional: handy for UI mapping
}
