package in.co.bel.acempp.dto.rawmaterialsourcing;


import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RmpFormDetailsResponse {
    private String                   formId;
    private String                   batchId;
    private Long                     subDepartmentId;
    private List<MaterialDetailDto> materials;
    private RmpProgressInsightsDto   progressInsights;
    private RmpQualityInsightsDto    qualityInsights;
    private RmpWorkflowInsightsDto   workflowInsights;
}