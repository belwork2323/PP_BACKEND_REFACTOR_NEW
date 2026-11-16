package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;

import in.co.bel.acempp.dto.batch.StatMetric;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManagerDashboardStatsResponse {

    private StatMetric totalBatches;
    private StatMetric inProgressBatches;
    private StatMetric completedBatches;
    private StatMetric pendingApprovals;
    private StatMetric rejectedBatches;
}