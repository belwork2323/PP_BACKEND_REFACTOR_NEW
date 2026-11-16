package in.co.bel.acempp.dto.rawmaterialpreparation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FormDetailsRequest {
    @NotBlank
    private String formId;
}