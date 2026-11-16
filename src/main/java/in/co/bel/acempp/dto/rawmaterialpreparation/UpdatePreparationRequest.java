package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdatePreparationRequest {
    @NotBlank
    private String formId;

    @NotNull
    private Status formSubmissionType;          // DRAFT / SUBMIT

    @NotNull @Valid
    private PreparationDetailsRequest preparationDetails;
}