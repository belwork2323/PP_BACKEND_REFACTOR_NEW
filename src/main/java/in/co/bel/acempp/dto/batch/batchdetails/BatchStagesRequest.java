package in.co.bel.acempp.dto.batch.batchdetails;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BatchStagesRequest {
    @NotBlank
    private String batchId;
}