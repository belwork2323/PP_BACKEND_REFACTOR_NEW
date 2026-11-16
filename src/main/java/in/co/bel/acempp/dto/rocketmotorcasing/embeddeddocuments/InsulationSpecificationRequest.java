package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InsulationSpecificationRequest {

    @NotBlank(message = "insulationType is required")
    private String insulationType;
}