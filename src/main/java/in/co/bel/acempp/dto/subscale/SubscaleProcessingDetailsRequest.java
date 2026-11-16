package in.co.bel.acempp.dto.subscale;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscaleProcessingDetailsRequest {
    @NotBlank
    private String formId;
}