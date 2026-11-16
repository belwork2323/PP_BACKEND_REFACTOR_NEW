package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ManagerChartDataResponse {
    private String               filterType;
    private String               timeUnit;
    private long                 totalActiveBatches;
    private List<ChartPoint> weeklyActivity;
    private List<ChartPoint>     motorsProcessed;
    private StageProcessedResponse stageProcessed;
}