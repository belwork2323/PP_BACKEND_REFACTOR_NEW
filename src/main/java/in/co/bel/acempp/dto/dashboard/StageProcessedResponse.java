package in.co.bel.acempp.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StageProcessedResponse {
    private long totalBatches;
    private List<DepartmentProgressDto> stages;
}