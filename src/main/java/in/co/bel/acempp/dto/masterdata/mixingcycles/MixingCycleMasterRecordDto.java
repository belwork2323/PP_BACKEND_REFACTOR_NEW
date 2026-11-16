package in.co.bel.acempp.dto.masterdata.mixingcycles;

import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParams;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingCycleMasterRecordDto {
    private Long id;
    private String mixingCycleCode;
    private String mixingCycleName;
    private String description;
    private String projectId;
    private Integer motorStage;
    private String motorStageName;
    private Boolean isActive;
    private CycleGroupDto cycles;
    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CycleGroupDto {
        @Builder.Default
        private List<OperationDto> premixOperations = new ArrayList<>();
        @Builder.Default
        private List<OperationDto> finalMixOperations = new ArrayList<>();
        @Builder.Default
        private List<QualityCheckParams> premixQualityChecks = new ArrayList<>();
        @Builder.Default
        private List<QualityCheckParams> finalMixQualityChecks = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationDto {
        private Long operationId;
        private Integer sequenceNo;
        private String operationName;
        private Boolean isActive;
    }
}
