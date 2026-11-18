package in.co.bel.acempp.dto.batch;

import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.CreateGroup;
import in.co.bel.acempp.validation.groups.UpdateGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BatchRequest {
    @NotBlank(message = "Batch ID is required", groups = {UpdateGroup.class})
    @Size(max = ValidationConstants.MAX_SHORT, groups = {UpdateGroup.class})
    private String batchId;

    @NotBlank(message = "Batch Type is required", groups = {CreateGroup.class, UpdateGroup.class})
    @Pattern(
            regexp = "^(MAIN|SUBSCALE)$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Batch Type must be MAIN or SUBSCALE",
            groups = {CreateGroup.class, UpdateGroup.class}
    )
    private String batchType;

    @Pattern(
            regexp = "^(QUALIFICATION|EXPERIMENTAL)$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Sub-Batch Type must be QUALIFICATION or EXPERIMENTAL",
            groups = {CreateGroup.class, UpdateGroup.class}
    )
    private String subBatchType;

    @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class})
    private String projectId;

    private Integer motorStage;

    @Min(value = 0, message = "Number of motors cannot be negative", groups = {CreateGroup.class, UpdateGroup.class})
    @Max(value = 10, message = "Maximum 10 motors allowed per batch", groups = {CreateGroup.class, UpdateGroup.class})
    private Integer numberOfMotors;

    private List<
                @NotBlank(message = "Motor ID cannot be blank", groups = {CreateGroup.class, UpdateGroup.class})
                @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class})
                        String
                > motorIds;

    @Valid
    private List<@NotNull(groups = {CreateGroup.class, UpdateGroup.class}) SubscaleArticleRequest> articles;

    @Size(
            max = ValidationConstants.MAX_LONG_TEXT,
            message = "Objective cannot exceed 500 characters",
            groups = {CreateGroup.class, UpdateGroup.class}
    )
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Invalid characters in objective",
            groups = {CreateGroup.class, UpdateGroup.class}
    )
    private String objective;

    @NotBlank(message = "Priority is required", groups = {CreateGroup.class, UpdateGroup.class})
    @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class})
    private String priority;

    @Size(max = ValidationConstants.MAX_SHORT, groups = {CreateGroup.class, UpdateGroup.class})
    private String systemManagerId;

    @Valid
    private IdentificationSheetDto identificationSheet;
}
