package in.co.bel.acempp.dto.ndt.motor;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalExposureDetailsDto {

    @NotNull(message = "Section number is required", groups = SubmitGroup.class)
    private Integer sectionNumber;

    @NotBlank(message = "Orientation is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Orientation contains invalid characters",
            groups = SubmitGroup.class
    )
    private String orientation;

    @NotNull(message = "Number of exposures is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "Number of exposures must be non-negative", groups = SubmitGroup.class)
    private Integer numberOfExposure;
}