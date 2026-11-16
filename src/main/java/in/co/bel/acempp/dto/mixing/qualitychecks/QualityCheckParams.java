package in.co.bel.acempp.dto.mixing.qualitychecks;

import in.co.bel.acempp.dto.user.operations.SpecificationRangeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckParams {
    private String parameterId;

    private String parameterName;

    private SpecificationRangeDto specification;

    private Integer noOfSamples;

    /** Null treated as active for parameters created before soft-disable. */
    private Boolean isActive;
}
