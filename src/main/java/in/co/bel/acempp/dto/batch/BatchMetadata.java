package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.dto.castingcuring.CuringMetadata;
import in.co.bel.acempp.dto.mixing.MixingMetadata;
import in.co.bel.acempp.dto.rawmaterialpreparation.RMPMetadata;
import in.co.bel.acempp.dto.subscale.SubScaleMetadata;
import in.co.bel.acempp.dto.rocketmotorcasing.RocketMotorListdata;
import in.co.bel.acempp.entity.manufacturing.CuringCycleMaster;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMetadata {

    private RMPMetadata rawMaterialPreparation;
    private RocketMotorListdata rocketMotorCasing;
    private MixingMetadata mixing;
    private SubScaleMetadata subScale;
    private CuringMetadata curingCycleMaster;

}
