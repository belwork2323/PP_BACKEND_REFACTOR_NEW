package in.co.bel.acempp.dto.masterdata.mixingcycles;

import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
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
public class MixingCycleMasterCreateRequest {
    @Size(max = 64)
    private String mixingCycleCode;

    @NotBlank
    private String projectId;

    @NotNull
    private Integer motorStage;

    private String motorStageName;

    @NotBlank
    @Size(max = 255)
    private String mixingCycleName;

    private String description;

    private Boolean isActive;

    @Valid
    private CycleGroupWriteDto cycles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CycleGroupWriteDto {
        @Valid
        @Builder.Default
        private List<OperationWriteDto> premixOperations = new ArrayList<>();
        @Valid
        @Builder.Default
        private List<OperationWriteDto> finalMixOperations = new ArrayList<>();
        @Valid
        @Builder.Default
        private List<QualityCheckParams> premixQualityChecks = new ArrayList<>();
        @Valid
        @Builder.Default
        private List<QualityCheckParams> finalMixQualityChecks = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationWriteDto {
        private Long operationId;
        private Integer sequenceNo;
        @NotBlank
        private String operationName;
        private Boolean isActive;
    }
}
