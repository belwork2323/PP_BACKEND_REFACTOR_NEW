package in.co.bel.acempp.dto.ndt.motor;

import in.co.bel.acempp.validation.ValidationConstants;
import in.co.bel.acempp.validation.groups.SubmitGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiographyDetailsDto {

    @NotEmpty(message = "Equipment utilized is required", groups = SubmitGroup.class)
    private List<@NotBlank(message = "Equipment entry must not be blank", groups = SubmitGroup.class) String> equipmentUtilized;

    @NotEmpty(message = "X-ray beam energies are required", groups = SubmitGroup.class)
    private List<@NotBlank(message = "Beam energy must not be blank", groups = SubmitGroup.class) String> xrayBeamEnergies;

    @Size(max = ValidationConstants.CODE_REF_MAX, message = "Radiography plan ID must be at most 50 characters", groups = SubmitGroup.class)
    private String radiographyPlanId;

    @Valid
    @NotEmpty(message = "At least one radiography plan detail row is required", groups = SubmitGroup.class)
    private List<RadiographyPlanDetailsDto> radiographyPlanDetails;
}