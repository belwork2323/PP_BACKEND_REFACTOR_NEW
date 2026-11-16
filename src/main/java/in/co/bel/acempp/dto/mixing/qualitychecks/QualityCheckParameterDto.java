package in.co.bel.acempp.dto.mixing.qualitychecks;

import in.co.bel.acempp.dto.user.operations.SpecificationRangeDto;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class QualityCheckParameterDto {

    @NotBlank(message = "Parameter ID is required", groups = SubmitGroup.class)
    @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
            message = "Parameter ID must be alphanumeric", groups = {DraftGroup.class, SubmitGroup.class})
    @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
            message = "Parameter ID cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
    private String parameterId;

    private String parameterName;

    private SpecificationRangeDto specification;

    /** Mixing Cycle Master sample count — persists with manufacturing Mixing quality checks. */
    @Min(value = 1, message = "Number of samples must be at least 1",
            groups = {DraftGroup.class, SubmitGroup.class})
    private Integer noOfSamples;

    @Valid
    @NotEmpty(message = "Quality observations cannot be empty", groups = SubmitGroup.class)
    private List<@NotNull(message = "Quality observation is required", groups = SubmitGroup.class)
    @Valid QualityObservationDto> observations;
}
