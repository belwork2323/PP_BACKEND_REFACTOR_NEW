package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

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

    private List<ChartPointDto> weeklyActivity;

    private List<ChartPointDto> motorsProcessed;

    private List<StageProcessedDto> stageProcessed;
}