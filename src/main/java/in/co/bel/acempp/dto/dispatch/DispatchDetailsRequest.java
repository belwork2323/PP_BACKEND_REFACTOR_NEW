package in.co.bel.acempp.dto.dispatch;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DispatchDetailsRequest {
    @NotBlank
    private String formId;
}