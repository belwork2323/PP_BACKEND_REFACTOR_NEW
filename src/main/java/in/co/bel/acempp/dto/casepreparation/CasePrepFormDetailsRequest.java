package in.co.bel.acempp.dto.casepreparation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CasePrepFormDetailsRequest {
    @NotBlank
    private String formId;
}