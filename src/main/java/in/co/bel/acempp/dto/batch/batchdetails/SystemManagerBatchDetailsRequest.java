package in.co.bel.acempp.dto.batch.batchdetails;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SystemManagerBatchDetailsRequest {
    @NotBlank
    private String batchId;
    @NotNull
    private Long subDepartmentId;
}