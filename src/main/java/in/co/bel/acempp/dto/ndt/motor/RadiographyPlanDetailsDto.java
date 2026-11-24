package in.co.bel.acempp.dto.ndt.motor;

import com.fasterxml.jackson.annotation.JsonAlias;
import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiographyPlanDetailsDto {

    private Integer srNo;

    @NotNull(message = "Sections is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "Sections must be a valid number", groups = SubmitGroup.class)
    @JsonAlias("noOfSections")
    private Integer numberOfSections;

    @NotNull(message = "Orientations is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "Orientations must be a valid number", groups = SubmitGroup.class)
    @JsonAlias("noOfOrientations")
    private BigDecimal numberOfOrientations;

    @NotNull(message = "SFD is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "SFD must be a valid number", groups = SubmitGroup.class)
    private BigDecimal sfd;


    @NotNull(message = "Normal exposure is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "Normal exposure must be a valid number", groups = SubmitGroup.class)
    @JsonAlias({"normalExposures", "noOfNormalExposures"})
    private BigDecimal numberOfNormalExposures;

    @NotNull(message = "Tangential exposure is required", groups = SubmitGroup.class)
    @PositiveOrZero(message = "Tangential exposure must be a valid number", groups = SubmitGroup.class)
    @JsonAlias({"tangentialExposures", "noOfTangentialExposures"})
    private BigDecimal numberOfTangentialExposures;

    @NotBlank(message = "Detector type is required", groups = SubmitGroup.class)
    @Size(max = ValidationConstants.MAX_STANDARD, groups = SubmitGroup.class)
    @Pattern(
            regexp = ValidationConstants.ALPHABET_WITH_SPECIAL,
            message = "Detector type contains invalid characters",
            groups = SubmitGroup.class
    )
    private String detectorType;
    private String qaRemarks;
}
