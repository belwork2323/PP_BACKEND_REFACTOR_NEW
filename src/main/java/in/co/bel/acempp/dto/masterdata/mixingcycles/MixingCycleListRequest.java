package in.co.bel.acempp.dto.masterdata.mixingcycles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingCycleListRequest {
    private String search;
    private Boolean isActive;
    private String projectId;
    private Integer motorStage;
}
