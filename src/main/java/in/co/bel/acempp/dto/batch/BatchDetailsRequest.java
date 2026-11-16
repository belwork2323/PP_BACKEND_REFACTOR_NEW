package in.co.bel.acempp.dto.batch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BatchDetailsRequest {
    @NotBlank
    private String batchId;

    private String assetId;
}