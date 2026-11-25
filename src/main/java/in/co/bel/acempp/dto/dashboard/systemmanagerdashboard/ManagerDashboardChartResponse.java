package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import in.co.bel.acempp.dto.dashboard.ChartPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ManagerDashboardChartResponse {

    private String filterType;

    private String timeUnit;

    private Long totalActiveBatches;

    private List<ChartPoint> weeklyActivity;

    private List<ChartPoint> motorsProcessed;

    private List<StageProcessedDto> stageProcessed;
}