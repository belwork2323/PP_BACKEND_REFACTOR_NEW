package in.co.bel.acempp.dto.admin.motor;

import in.co.bel.acempp.dto.batch.StatMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorStatsResponse {
    private StatMetric totalMotors;
    private StatMetric inProgress;
    private StatMetric dispatched;
    private StatMetric discarded;
}
