package in.co.bel.acempp.dto.batch;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchStatsResponse {
    private StatMetric totalBatches;
    private StatMetric inProgressBatches;
    private StatMetric completedBatches;
    private StatMetric pendingApprovals;
    private StatMetric rejectedBatches;
}