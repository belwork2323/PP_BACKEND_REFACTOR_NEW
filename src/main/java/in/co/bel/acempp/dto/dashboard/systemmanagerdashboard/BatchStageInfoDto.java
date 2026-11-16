package in.co.bel.acempp.dto.dashboard.systemmanagerdashboard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStageInfoDto {

    private StageEntryDto lastUpdatedStage;

    private List<StageEntryDto> currentStage;

    private List<StageEntryDto> stageHistory;
}