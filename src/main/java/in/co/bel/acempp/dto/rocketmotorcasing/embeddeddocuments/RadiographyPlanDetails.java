package in.co.bel.acempp.dto.rocketmotorcasing.embeddeddocuments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadiographyPlanDetails {
    private Integer numberOfSections;

    private Integer numberOfOrientations;

    private Integer sfd;

    private Integer numberOfNormalExposures;

    private Integer numberOfTangentialExposures;

    private String detectorType;
}
