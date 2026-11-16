package in.co.bel.acempp.dto.ndt.motor;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiographyPlanDetailsDto {

    private Integer srNo;

    @JsonAlias("noOfSections")
    private Integer numberOfSections;

    @JsonAlias("noOfOrientations")
    private Integer numberOfOrientations;

    private Integer sfd;

    @JsonAlias({"normalExposures", "noOfNormalExposures"})
    private Integer numberOfNormalExposures;

    @JsonAlias({"tangentialExposures", "noOfTangentialExposures"})
    private Integer numberOfTangentialExposures;

    private String detectorType;

    /** QA row remarks (QC-entered); ignored by manufacturing UI. */
    private String qaRemarks;
}
