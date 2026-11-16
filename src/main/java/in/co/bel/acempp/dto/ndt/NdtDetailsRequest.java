package in.co.bel.acempp.dto.ndt;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NdtDetailsRequest {
    @NotBlank
    private String formId;
}