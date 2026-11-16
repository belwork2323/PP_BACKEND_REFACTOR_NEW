package in.co.bel.acempp.dto.mixing.mixingcycle;

import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import in.co.bel.acempp.validation.groups.DetailGroup;
import in.co.bel.acempp.validation.groups.ListGroup;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MixingCycleListRequest {


    @NotBlank(message = "Motor stage is required", groups = {ListGroup.class})
    private String motorStage; // list

    @NotBlank(message = "Mixing cycle code is required", groups = {DetailGroup.class})
    private String mixingCycleCode;

    private String mixerType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MixingCycleDetailsResponse {

        private Long mixingCycleId;
        private String mixingCycleCode;
        private String mixingCycleName;
        private String description;
        private Integer motorStage;
        private CyclesDto cycles;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CyclesDto {
            private List<OperationDto> premixOperations;
            private List<OperationDto> finalMixOperations;
            private List<QualityCheckParams> premixQualityChecks;
            private List<QualityCheckParams> finalMixQualityChecks;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OperationDto {
            private Long operationId;
            private Integer sequenceNo;
            private String operationName;
        }
    }
}