package in.co.bel.acempp.dto.mixing.mixingform;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers;
import in.co.bel.acempp.dto.general.PersonInfo;
import in.co.bel.acempp.dto.mixing.MixDetailsDto;
import in.co.bel.acempp.dto.mixing.MixerConfigurationDto;
import in.co.bel.acempp.dto.mixing.mixingcycle.MixingCycleDto;
import in.co.bel.acempp.dto.mixing.processparticulars.ProcessParticularRowDto;
import in.co.bel.acempp.dto.mixing.qualitychecks.QualityCheckParameterDto;
import in.co.bel.acempp.enums.Status;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.PremixStageSubmitGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremixDto {

    @NotNull(message = "Premix number is required",
            groups = {CreateGroup.class, UpdateGroup.class, SubmitGroup.class})
    @Positive(message = "Premix number must be greater than 0",
            groups = {CreateGroup.class, UpdateGroup.class, DraftGroup.class, SubmitGroup.class})
    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberIntegerValue.class)
    private Integer premixNo;

    private Status premixSubmissionType;
    private Status premixSubmissionStatus;

    @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
            message = "Remarks cannot exceed 255 characters", groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
            message = "Remarks contain invalid characters", groups = {DraftGroup.class, SubmitGroup.class})
    private String remarks;
    @Size(max = ValidationConstants.REMARKS_OBSERVATIONS_MAX,
            message = "Rejection reason cannot exceed 255 characters", groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_DESCRIPTIVE_TEXT_PATTERN,
            message = "Rejection reason contains invalid characters", groups = {DraftGroup.class, SubmitGroup.class})
    private String rejectionReason;

    private PersonInfo actionBy;
    private LocalDateTime actionAt;

    @Valid
    @NotNull(message = "Mixer configuration is required", groups = SubmitGroup.class)
    private MixerConfigurationDto mixerConfiguration;

    @Valid
    @NotNull(message = "Trial details are required", groups = PremixStageSubmitGroup.class)
    private TrialDetailsDto trialDetails;

    @Valid
    @NotNull(message = "Mix details are required", groups = PremixStageSubmitGroup.class)
    private MixDetailsDto mixDetails;

    @Valid
    @NotNull(message = "Mixing cycle is required", groups = SubmitGroup.class)
    private MixingCycleDto mixingCycle;

    @NotEmpty(message = "Process particulars cannot be empty", groups = SubmitGroup.class)
    private List<@NotNull(message = "Process particular is required", groups = SubmitGroup.class)
    @Valid ProcessParticularRowDto> processParticulars;

    @NotEmpty(message = "Quality checks cannot be empty", groups = SubmitGroup.class)
    private List<@NotNull(message = "Quality check is required", groups = SubmitGroup.class)
    @Valid QualityCheckParameterDto> qualityChecks;
}
