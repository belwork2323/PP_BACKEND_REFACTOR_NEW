package in.co.bel.acempp.dto.subscale;

import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubScaleMetadata {
    private HardwarePreparationDetailsDto hardwarePreparationDetails;

    private List<BemMotorDto> bemMotors;

    /** Mixing cycle selected on the subscale manufacturing form (for QC Mixing MDM rows). */
    private MixingCycleDto mixingCycle;
}