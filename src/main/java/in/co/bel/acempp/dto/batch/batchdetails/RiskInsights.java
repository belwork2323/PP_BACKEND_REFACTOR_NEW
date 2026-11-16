package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

import java.util.List;

@Data
@Builder
public class RiskInsights {
    private String riskLevel;
    private int riskScore;
    private List<String> riskFactors;
}