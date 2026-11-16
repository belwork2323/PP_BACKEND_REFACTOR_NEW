package in.co.bel.acempp.dto.mixing;

import in.co.bel.acempp.enums.StageType;
import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingStageMetadata {

    private StageType stageType;

    private List<ApprovedMixMetadata> premixes = new ArrayList<>();
}