package in.co.bel.acempp.dto.batch.batchdetails;

import lombok.*;

import java.util.List;

@Data
@Builder
public class QualityInsights {
    private int totalTests;
    private int passed;
    private int failed;
    private double passRate;
    private List<NonConformance> nonConformances;
}