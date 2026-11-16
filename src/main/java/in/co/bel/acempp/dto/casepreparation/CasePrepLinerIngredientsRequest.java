package in.co.bel.acempp.dto.casepreparation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CasePrepLinerIngredientsRequest {
    /** PEDCOAT | HEMCOAT_3L | HEMCOAT_3L_M | OTHERS */
    @NotBlank
    private String linerType;
}
