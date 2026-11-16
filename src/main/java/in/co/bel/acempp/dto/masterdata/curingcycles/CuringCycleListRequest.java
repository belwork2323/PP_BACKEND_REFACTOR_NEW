package in.co.bel.acempp.dto.masterdata.curingcycles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuringCycleListRequest {
    private String search;
    private Boolean isActive;
    private String projectId;
    private Integer motorStage;
}
