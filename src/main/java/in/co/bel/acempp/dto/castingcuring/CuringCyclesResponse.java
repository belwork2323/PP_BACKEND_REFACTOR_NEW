package in.co.bel.acempp.dto.castingcuring;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CuringCyclesResponse {

    private int motorStage;
    private String motorStageName;
    private String curingType;
    private List<Cycle> cycles;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Cycle {

        private int sequenceNo;
        private int temperature;
        private int durationMinutes;

        private LocalDateTime startDateTime;

        private LocalDateTime endDateTime;

        private String hotWaterCirculation;
    }
}
