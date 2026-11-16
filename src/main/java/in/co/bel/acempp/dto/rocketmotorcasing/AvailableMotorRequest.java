package in.co.bel.acempp.dto.rocketmotorcasing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailableMotorRequest {

    @NotBlank
    private String projectId;

    @NotNull
    @Min(0)
    private Integer motorStage;
}
