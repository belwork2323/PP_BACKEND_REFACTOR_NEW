package in.co.bel.acempp.dto.masterdata.qualitychecks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckListRequest {
    private String search;
    private Boolean isActive;
    private String mixType;
    private Integer motorStage;
}
