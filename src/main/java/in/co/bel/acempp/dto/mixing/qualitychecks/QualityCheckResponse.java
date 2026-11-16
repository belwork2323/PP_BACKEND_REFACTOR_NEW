package in.co.bel.acempp.dto.mixing.qualitychecks;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QualityCheckResponse {
    private String mixType;

    private List<QualityCheckParams> qualityChecks;


}
