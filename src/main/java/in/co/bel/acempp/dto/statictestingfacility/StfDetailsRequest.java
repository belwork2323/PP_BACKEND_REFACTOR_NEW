package in.co.bel.acempp.dto.statictestingfacility;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StfDetailsRequest {
    @NotBlank
    private String formId;
}