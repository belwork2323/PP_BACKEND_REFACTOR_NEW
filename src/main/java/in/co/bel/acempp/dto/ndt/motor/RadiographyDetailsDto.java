package in.co.bel.acempp.dto.ndt.motor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiographyDetailsDto {

    private List<String> equipmentUtilized;

    private List<String> xrayBeamEnergies;

    private String radiographyPlanId;

    private List<RadiographyPlanDetailsDto> radiographyPlanDetails;
}