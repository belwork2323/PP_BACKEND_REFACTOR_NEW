package in.co.bel.acempp.dto.masterdata.curingcycles;

import in.co.bel.acempp.dto.general.PersonInfo;
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
public class CuringCycleMasterRecordDto {
    private Long id;
    private String curingCycleCode;
    private String projectId;
    private Integer motorStage;
    private String motorStageName;
    private String curingType;
    private Boolean isActive;
    @Builder.Default
    private List<CycleDto> cycles = new ArrayList<>();

    private PersonInfo createdBy;
    private LocalDateTime createdOn;
    private PersonInfo updatedBy;
    private LocalDateTime updatedOn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CycleDto {
        private Long stepId;
        private Integer sequenceNo;
        private Integer temperature;
        private Integer durationMinutes;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        private String hotWaterCirculation;
        private Boolean isActive;
    }
}
