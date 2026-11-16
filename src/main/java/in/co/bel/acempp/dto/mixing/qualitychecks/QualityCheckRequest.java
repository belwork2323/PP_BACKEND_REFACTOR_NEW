package in.co.bel.acempp.dto.mixing.qualitychecks;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QualityCheckRequest {
    @NotBlank(message = "mixer Type is required")
    private String mixType;
    @NotNull(message = "motor stage is required")
    private Integer motorStage;
    /** Optional: preferred when multiple active cycles exist for a stage. */
    private String mixingCycleCode;
}
