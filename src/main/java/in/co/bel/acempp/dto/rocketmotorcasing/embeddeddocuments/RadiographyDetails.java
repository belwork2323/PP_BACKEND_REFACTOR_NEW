package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import in.co.bel.acempp.dto.ndt.motor.RadiographyPlanDetailsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadiographyDetails {

    private String radiographyPlanId;

    private String radiographyPlanName;

    private List<RadiographyPlanDetailsDto> radiographyPlanDetails;

}
