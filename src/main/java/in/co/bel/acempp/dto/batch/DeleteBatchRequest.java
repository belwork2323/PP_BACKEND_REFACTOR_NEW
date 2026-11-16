package in.co.bel.acempp.dto.batch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteBatchRequest {
    @NotBlank
    private String batchId;
    @NotBlank private String reason;
}
