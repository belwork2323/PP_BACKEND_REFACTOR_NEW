package in.co.bel.acempp.dto.mixing;

import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MixerConfigurationDto {

    @NotBlank(message = "Mixer ID is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "Mixer ID cannot exceed 100 characters",
            groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
            message = "Mixer ID format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
    private String mixerId;

    @NotBlank(message = "Building number is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "Building number cannot exceed 100 characters",
            groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
            message = "Building number format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
    private String bldgNo;

    @NotBlank(message = "Bowl ID is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "Bowl ID cannot exceed 100 characters",
            groups = {DraftGroup.class, SubmitGroup.class})
    private String bowlId;
}