package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RmpStatsResponse {
    private RmpKpisDto kpis;
    private RmpMyPerformanceDto myPerformance;
    private List<MaterialDistributionDto> materialDistribution;
    private ManufacturerInsightsDto manufacturerInsights;
}