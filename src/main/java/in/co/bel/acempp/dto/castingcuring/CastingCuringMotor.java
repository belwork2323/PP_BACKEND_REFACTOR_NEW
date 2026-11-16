package in.co.bel.acempp.dto.castingcuring;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.CastingSections;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.CastingSetup;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.CuringSections;
import in.co.bel.acempp.dto.castingcuring.CastingCuringSectionPayloads.CuringSetup;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
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

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CastingCuringMotor {
    @NotBlank(message = "motorId is required", groups = {DraftGroup.class, SubmitGroup.class})
    @Size(max = ValidationConstants.STANDARD_TEXT_MAX, message = "motorId cannot exceed 100 characters", groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN, message = "motorId format is invalid", groups = {DraftGroup.class, SubmitGroup.class})
    private String motorId;

    @NotNull(message = "motorReceivedAt is required", groups = {DraftGroup.class, SubmitGroup.class})
    private LocalDateTime motorReceivedAt;

    private Status motorSubmissionType;
    private Status motorSubmissionStatus; // Present in response/model

    @Valid
    private CastingSetup setup;

    @Valid
    @NotNull(message = "curingSetup is required", groups = SubmitGroup.class)
    private CuringSetup curingSetup;

    @Valid
    @NotNull(message = "castingSections is required", groups = SubmitGroup.class)
    private CastingSections castingSections;

    @Valid
    @NotNull(message = "curingSections is required", groups = SubmitGroup.class)
    private CuringSections curingSections;
}
