package in.co.bel.acempp.dto.rawmaterialsourcing;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RmpProgressInsightsDto {
    private int     materialsCount;
    private int     totalSpecs;
    private int     filledSpecs;
    private int     completionPercentage;
    private boolean allLotsEntered;
    private boolean isReadyForApproval;
}