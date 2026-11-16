package in.co.bel.acempp.dto.masterdata.curingcycles;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CuringCycleMasterCreateRequest {
    @NotBlank
    private String projectId;

    @NotNull
    private Integer motorStage;

    private String motorStageName;

    @NotBlank
    private String curingType;

    private Boolean isActive;

    @Valid
    @Builder.Default
    private List<CuringCycleMasterRecordDto.CycleDto> cycles = new ArrayList<>();
}
