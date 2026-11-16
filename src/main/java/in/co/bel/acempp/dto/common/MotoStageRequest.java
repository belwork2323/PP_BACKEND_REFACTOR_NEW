package in.co.bel.acempp.dto.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MotoStageRequest {
    @NotBlank
    private String projectId;

    @NotNull
    private Integer motorStage;

    @NotBlank
    private String curingType;
}
