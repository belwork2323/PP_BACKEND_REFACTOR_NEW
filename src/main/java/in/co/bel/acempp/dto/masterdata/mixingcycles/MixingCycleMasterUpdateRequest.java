package in.co.bel.acempp.dto.masterdata.mixingcycles;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingCycleMasterUpdateRequest {
    @NotNull
    private Long id;

    private String mixingCycleCode;

    private String projectId;

    private Integer motorStage;

    private String motorStageName;

    @NotBlank
    @Size(max = 255)
    private String mixingCycleName;

    private String description;

    @NotNull
    private Boolean isActive;

    @Valid
    private MixingCycleMasterCreateRequest.CycleGroupWriteDto cycles;
}
