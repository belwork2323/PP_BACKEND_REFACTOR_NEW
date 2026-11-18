package in.co.bel.acempp.dto.castingcuring;

import in.co.bel.acempp.entity.manufacturing.CuringCycleMaster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CuringMetadata {
    private Long masterId;
    private String curingCycleCode;
    private String curingType;
    private List<CuringCycleMaster.Cycle> cycles;

}
