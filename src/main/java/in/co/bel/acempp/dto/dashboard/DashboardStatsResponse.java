package in.co.bel.acempp.dto.dashboard;

import in.co.bel.acempp.dto.batch.StatMetric;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private StatMetric totalUsers;
    private StatMetric activeUsers;
    private StatMetric completedBatches;
    private StatMetric openBatches;
//    private StatMetric motorsDispatched;
//    private StatMetric pendingApprovals;
}
