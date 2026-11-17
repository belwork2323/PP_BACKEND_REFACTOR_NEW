package in.co.bel.acempp.dto.subscale.subscalecreation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
public class SubscaleDetailsDTO {
    private Integer batchSize;
    private LocalDate premixDate;
    private String mixerType;

    private String bldgNo;

    private LocalDate finalMixDate;

    // Fixed: Changed from String to MixingCycle object to match JSON payload structure
    private MixingCycle mixingCycle;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MixingCycle {
        private Integer motorStage;
        private Integer mixingCycleId;
        private String mixingCycleCode;
        private String mixingCycleName;
        private List<MixParticular> premixParticulars;
        private List<MixParticular> finalMixParticulars;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MixParticular {
        private Integer operationId;
        private String operation;
        private String rpm;
        private String time;
        private String temp;
        private String vacuum;
    }

}
