package in.co.bel.acempp.dto.mixing;

import lombok.*;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingMetadata {

    private List<MixingStageMetadata> stages = new ArrayList<>();
}