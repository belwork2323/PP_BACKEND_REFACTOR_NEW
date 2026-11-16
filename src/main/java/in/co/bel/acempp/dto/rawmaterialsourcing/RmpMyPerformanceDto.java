package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RmpMyPerformanceDto {
    private long pendingActions;
    private long lotsProcessed;
    private double departmentContribution;
    private double avgTurnaroundTimeDays;
}