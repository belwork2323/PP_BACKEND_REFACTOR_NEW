package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

import java.util.List;

@Data
@Builder
public class ProgressInsights {
    private int totalSpecs;
    private int filledSpecs;
    private int completionPercentage;
    private boolean isReadyForApproval;
    private List<StageSummary> stageSummary;   // new
}