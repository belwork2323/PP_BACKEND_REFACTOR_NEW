package in.co.bel.acempp.dto.mixing.mixingcycle;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingCycleInfo {

    private Long id;

    private String mixingCycleCode;

    private String mixingCycleName;

    private Integer motorStage;
}