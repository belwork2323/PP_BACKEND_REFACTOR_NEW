package in.co.bel.acempp.dto.mixing.mixingcycle;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import in.co.bel.acempp.dto.common.StrictScalarDeserializers;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.DraftGroup;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MixingCycleDto {

    @Positive(message = "Mixing cycle ID must be greater than 0", groups = {DraftGroup.class, SubmitGroup.class})
//    @JsonDeserialize(using = StrictScalarDeserializers.JsonNumberLongValue.class)
    private Long mixingCycleId;

    @NotBlank(message = "Mixing cycle code is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
            message = "Mixing cycle code cannot exceed 100 characters",
            groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
            message = "Mixing cycle code format is invalid",
            groups = {DraftGroup.class, SubmitGroup.class})
    private String mixingCycleCode;

    @Size(max = ValidationConstants.STANDARD_TEXT_MAX,
            message = "Mixing cycle name cannot exceed 100 characters",
            groups = {DraftGroup.class, SubmitGroup.class})
    @Pattern(regexp = ValidationConstants.OPTIONAL_BUSINESS_TEXT_PATTERN,
            message = "Mixing cycle name format is invalid",
            groups = {DraftGroup.class, SubmitGroup.class})
    private String mixingCycleName;

}
