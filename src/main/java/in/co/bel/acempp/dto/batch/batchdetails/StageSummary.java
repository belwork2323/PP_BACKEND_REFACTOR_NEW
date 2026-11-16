package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

@Data @Builder
public class StageSummary {
    private String stageType;
    private int totalPremixes;
    private int totalFields;
    private int filledFields;
    private int completionPercentage;
}