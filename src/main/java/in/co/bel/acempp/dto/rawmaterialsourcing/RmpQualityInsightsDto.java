package in.co.bel.acempp.dto.rawmaterialsourcing;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class RmpQualityInsightsDto {
    private int          totalTests;
    private int          passed;
    private int          failed;
    private Double          passRate;
    private List<String> nonConformances;
}