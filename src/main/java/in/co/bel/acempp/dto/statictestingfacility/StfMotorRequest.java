package in.co.bel.acempp.dto.statictestingfacility;

import in.co.bel.acempp.dto.statictestingfacility.StfPayloads.StaticTestingDetails;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.enums.StfMotorSubType;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StfMotorRequest {
    @NotBlank(message = "Motor ID is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.CODE_REF_MAX, groups = SubmitGroup.class)
    @Pattern(regexp = ValidationConstants.CODE_REF_PATTERN, groups = SubmitGroup.class)
    private String motorId;

    @NotNull(message = "Sub-type is required", groups = SubmitGroup.class)
    private StfMotorSubType subType;

    @NotBlank(message = "STF test number is required", groups = SubmitGroup.class)
    @Size(max = 30, message = "STF test number must be at most 30 characters", groups = SubmitGroup.class)
    @Pattern(regexp = ValidationConstants.CODE_REF_PATTERN, groups = SubmitGroup.class)
    private String stfTestNo;

    @NotNull(message = "Motor submission type is required", groups = SubmitGroup.class)
    private Status motorSubmissionType;

    @Valid
    @NotNull(message = "Static testing details are required", groups = SubmitGroup.class)
    private StaticTestingDetails staticTestingDetails;
}
