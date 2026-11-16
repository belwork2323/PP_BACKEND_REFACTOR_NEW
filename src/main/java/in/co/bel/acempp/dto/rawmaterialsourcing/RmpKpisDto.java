package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class RmpKpisDto {
    private long totalLotsCreated;
    private double avgTurnaroundTimeDays;
    private double approvalYieldPercentage;
    private long agingLots;
}