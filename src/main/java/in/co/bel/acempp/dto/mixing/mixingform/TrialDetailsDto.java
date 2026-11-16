package in.co.bel.acempp.dto.mixing.mixingform;

import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrialDetailsDto {
    @NotNull(message = "Bowl trial date is required", groups = SubmitGroup.class)
    private LocalDateTime trialDateTime;

    @NotBlank(message = "Bowl trial observations are required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
            message = "Bowl trial observations cannot exceed 255 characters",
            groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
            message = "Bowl trial observations format is invalid",
            groups = {DraftGroup.class, SubmitGroup.class})
    private String observations;
}
