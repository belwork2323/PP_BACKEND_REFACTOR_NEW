package in.co.bel.acempp.dto.rawmaterialpreparation;

import in.co.bel.acempp.enums.Status;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreatePrepRequest {
    @NotBlank
    private String batchId;
    @NotNull private Long subDepartmentId;
    @NotNull private Status formSubmissionType;   // DRAFT / SUBMIT
    private PreparationDetailsRequest preparationDetails;
}
