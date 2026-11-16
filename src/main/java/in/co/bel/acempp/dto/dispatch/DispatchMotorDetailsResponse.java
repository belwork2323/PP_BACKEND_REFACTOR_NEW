package in.co.bel.acempp.dto.dispatch;

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
public class DispatchMotorDetailsResponse {

    private String batchId;

    @Builder.Default
    private List<MotorSeed> motors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MotorSeed {
        private String motorId;
        /** Partial seed: castingDate, ndtClearance, propellantProperties only. */
        private DispatchPayloads.DispatchDetails dispatchDetails;
    }
}
